package com.example.plugins

import com.example.data.services.DatabaseUserModel
import com.example.data.services.TransactionService
import com.example.data.services.UsersService
import com.example.domain.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.resources.Resources
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.datetime.toKotlinLocalDateTime
import java.util.UUID

fun Application.configureRouting(usersService: UsersService, transactionService: TransactionService) {
    install(Resources)
    install(AutoHeadResponse)
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
    }

    routing {
        post("/login") {
            val email: Email
            val password: Password
            try {
                val request = call.receive<LoginRequest>()
                email = Email(request.email)
                password = Password(request.password)
            } catch (e: Exception) {
                respondWithErrorSlug(ErrorSlug.INVALID_LOGIN_CREDENTIALS)
                return@post
            }

            val isUserRegistered = usersService.isUserRegistered(email)
            val isPasswordCorrect = usersService.isPasswordValidLoginCredential(email, password)

            if (isUserRegistered && isPasswordCorrect) {
                val sessionUUID = UUID.randomUUID()
                val currenTime = java.time.LocalDateTime.now()
                val expireTime = currenTime.plusMinutes(15.toLong()).toKotlinLocalDateTime()

                val newSession = UserSession(uuid = sessionUUID, email = email, expireTime = expireTime)

                call.sessions.set(newSession)
                respondWithData(mapOf("userId" to sessionUUID.toString()))
            } else {
                respondWithErrorSlug(ErrorSlug.INVALID_LOGIN_CREDENTIALS)
            }
        }
        post("/request_login_with_bits") {
            val request = call.receive<EmailRequest>()

            try {
                val email = Email(request.email)

                val bitsToLogin = usersService.genLoginBitsIndexesOrThrow(email)
                respondWithData(mapOf("bits" to bitsToLogin))
            } catch (e: ErrorSlugException) {
                respondWithErrorSlug(e.slug)
            } catch (e: Exception) {
                respondWithErrorSlug(ErrorSlug.INVALID_LOGIN_CREDENTIALS)
            }
        }
        post("/login_with_bits") {
            val bitLoginRequest = call.receiveOrNull<LoginWithBitsRequest>()

            if (bitLoginRequest == null) {
                respondWithErrorSlug(ErrorSlug.INVALID_LOGIN_CREDENTIALS)
                return@post
            }

            val email: Email?
            val properlyIndexedLoginBits: Map<Int, Char>
            try {
                email = Email(bitLoginRequest.email)
                properlyIndexedLoginBits = bitLoginRequest.bits.mapKeys { it.key + 1 }
            } catch (e: Exception) {
                respondWithErrorSlug(ErrorSlug.INVALID_LOGIN_CREDENTIALS)
                return@post
            }

            val isLoginSuccessful = usersService.arePasswordBitesValidLoginCredentials(email, properlyIndexedLoginBits)

            if (isLoginSuccessful) {
                val token = "generateToken(email.value)"
                call.respond(ApiResponse(null, mapOf("token" to token)))
                respondWithData("userId" to token)
            } else {
                respondWithErrorSlug(ErrorSlug.INVALID_LOGIN_CREDENTIALS)
            }
        }
        post("/create_user") {
            try {
                val request = call.receive<RegisterRequest>()

                val newUser = NewUserModel(
                    name = request.name,
                    email = request.email,
                    password = request.password,
                )

                if (!usersService.createUserOrFalse(newUser)) {
                    respondWithErrorSlug(ErrorSlug.USER_ALREADY_EXISTS)
                } else {
                    respondWithOperationSuccess(true)
                }
            } catch (e: ErrorSlugException) {
                respondWithErrorSlug(e.slug)
            } catch (e: Exception) {
                respondWithErrorSlug(ErrorSlug.CREATE_USER_CREDENTIALS_INVALID)
            }
        }
        get("/logout") {
            try {
                call.sessions.clear<UserSession>()
                respondWithOperationSuccess(true)
            } catch (e: Exception) {
                respondWithOperationSuccess(false)
            }
        }
        post("/deposit") {
            val request = call.receive<DepositRequest>()

            try {
                val userSession = getCurrentUserSessionOrThrow()

                transactionService.depositMoneyOrThrow(userSession.email, request.amount)

                respondWithOperationSuccess(true)
            } catch (e: ErrorSlugException) {
                respondWithErrorSlug(e.slug)
            } catch (e: Exception) {
                respondWithErrorSlug(ErrorSlug.DEPOSIT_FAILED)
            }
        }

        route("/transactions") {
            post("/withdraw") {
                val request = call.receive<WithdrawRequest>()

                try {
                    val session = getCurrentUserSessionOrThrow()

                    transactionService.withdrawMoneyOrThrow(session.email, request.amount)
                    respondWithOperationSuccess(success = true)
                } catch (e: ErrorSlugException) {
                    respondWithErrorSlug(e.slug)
                } catch (e: Exception) {
                    respondWithOperationSuccess(success = false)
                }
            }

            post("/send_money") {
                val request = call.receive<SendMoneyRequest>()
                try {
                    val senderSession = getCurrentUserSessionOrThrow()
                    val recipientEmail = Email(request.recipientEmail)

                    transactionService.sendMoneyOrThrow(
                        senderEmail = senderSession.email,
                        recipentEmail = recipientEmail,
                        amount = request.amount
                    )
                    respondWithOperationSuccess(success = true)
                } catch (e: ErrorSlugException) {
                    respondWithErrorSlug(e.slug)
                } catch (e: Exception) {
                    respondWithOperationSuccess(success = false)
                }
            }
        }
    }
}