package com.example.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.example.data.services.DatabaseUserModel
import com.example.data.services.TransactionService
import com.example.data.services.UsersService
import com.example.domain.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.resources.Resources
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.*
import java.util.*

fun Application.configureRouting(usersService: UsersService, transactionService: TransactionService) {

    val emailAndJwtTokenMap: Map<Email, String> = emptyMap()

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
                if (e is CredentialException) {
                    call.respondBadRequest(e.message)
                } else {
                    call.respondBadRequest()
                }
                return@post
            }


            val canUserLogin = usersService.canUserLogin(email)
            val isPasswordCorrect = usersService.isPasswordValidLoginCredential(email, password)
            when {
                !canUserLogin -> {
                    call.respond(ApiResponse(ErrorSlugs.USER_NOT_FOUND, null))

//                    call.respondBadRequest(ErrorSlugs.USER_NOT_FOUND)
                    return@post
                }

                !isPasswordCorrect -> {
                    call.respond(ApiResponse(ErrorSlugs.INVALID_LOGIN_CREDENTIALS, null))
                    call.respondBadRequest(ErrorSlugs.INVALID_LOGIN_CREDENTIALS)
                    return@post
                }

                else -> {
                    val token = generateToken(email.value)
                    call.respond(ApiResponse(null, mapOf("token" to token)))
                }
            }

        }
        post("/request_login_with_bits") {
            val email: Email
            try {
                val request = call.receive<EmailRequest>()
                email = Email(request.email)
            } catch (e: Exception) {
                call.respondBadRequest(ErrorSlugs.INVALID_LOGIN_CREDENTIALS)
                return@post
            }

//            try {
//                val bitsToLogin = usersService.genLoginBitsIndexesOrThrow(email)
//                call.respond("bits" to bitsToLogin)
//            } catch (e: Exception) {
//                call.respondBadRequest(ErrorSlugs.INVALID_LOGIN_CREDENTIALS)
//            }
            try {
                val bitsToLogin = usersService.genLoginBitsIndexesOrThrow(email)
                call.respond(ApiResponse(null, mapOf("bits" to bitsToLogin)))
            } catch (e: Exception) {
                call.respond(ApiResponse(ErrorSlugs.INVALID_LOGIN_CREDENTIALS, null))
            }
        }
        post("/login_with_bits") {
            val bitLoginRequest = call.receiveOrNull<LoginWithBitsRequest>()

            if (bitLoginRequest == null) {
                call.respondBadRequest()
                return@post
            }

            val email: Email?
            val properlyIndexedLoginBits: Map<Int, Char>
            try {
                email = Email(bitLoginRequest.email)
                properlyIndexedLoginBits = bitLoginRequest.bits.mapKeys { it.key + 1 }
            } catch (e: Exception) {
                call.respond(ErrorSlugs.INVALID_LOGIN_CREDENTIALS)
                return@post
            }

            val isLoginSuccessful = usersService.arePasswordBitesValidLoginCredentials(email, properlyIndexedLoginBits)

            if (isLoginSuccessful) {
                val token = generateToken(email.value)
                call.respond(ApiResponse(null, mapOf("token" to token)))
                println("### responded successfully")
            } else {
                println("### responded bad")
                call.respond(ApiResponse(ErrorSlugs.INVALID_LOGIN_CREDENTIALS, mapOf("token" to null)))
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

                if (!usersService.createUser(newUser)) {
                    call.respond(ApiResponse("USER_ALREADY_EXISTS", "success" to true))
                }

                call.respond(ApiResponse(null, "success" to true))
            } catch (e: Exception) {
                if (e is CredentialException) {
                    call.respondBadRequest(e.message)
                } else {
                    call.respondBadRequest()
                }
                return@post
            }
        }
        authenticate("jwt") {
            get("/user/{id}") {
                try {
                    val userId = call.parameters["id"]?.toIntOrNull() ?: throw IllegalArgumentException("User with id not found")

                    val principal = call.principal<JWTPrincipal>()
                    val idData = usersService.getUserWithId(id = userId) ?: throw IllegalArgumentException("User with id not found")
                    if (idData.email != principal?.subject) {
                        throw IllegalArgumentException("No permission to view this user")
                    }
                    val response = PrivateUserModel(
                        name = idData.name,
                        email = idData.email,
                        balance = idData.balance,
                        accountNumber = "11200333-333"
                    )
                    call.respond(ApiResponse(null, response))
                } catch (e: Exception) {
                    call.respond(ApiResponse("Failed to get user because: ${e.message}", null))
                }
            }

            get("/user_id/{email}") {
                try {
                    val userEmail = call.parameters["email"] ?: throw IllegalArgumentException("User with email not found")

                    val userID = usersService.getUserIdWithEmail(userEmail)

                    call.respond(ApiResponse(null, userID))
                } catch (e: Exception) {
                    call.respond(ApiResponse("Failed to get user because: ${e.message}", null))
                }
            }


            get("/usersList") {
                try {
                    val users = usersService.getAll().map { it.asSafeUser() }
                    call.respond(ApiResponse("Get successful", users))
                } catch (e: Exception) {
                    call.respond(ApiResponse("Failed to deposit: ${e.message}", null))
                }
            }
            post("/deposit") {
                val request = call.receive<DepositRequest>()

                try {
                    transactionService.depositMoneyOrThrow(request.userId, request.amount)
                    call.respond(ApiResponse("Deposit successful", null))
                } catch (e: Exception) {
                    call.respond(ApiResponse("Failed to deposit: ${e.message}", null))
                }
            }

            route("/transactions") {

                post("/withdraw") {
                    val request = call.receive<WithdrawRequest>()

                    try {
                        transactionService.withdrawMoneyOrThrow(request.userId, request.amount)
                        call.respond(ApiResponse("Withdrawal successful", null))
                    } catch (e: Exception) {
                        call.respond(ApiResponse("Failed to withdraw: ${e.message}", null))
                    }
                }

                post("/send_money") {
                    val request = call.receive<SendMoneyRequest>()

                    try {
                        transactionService.sendMoneyOrThrow(request.senderId, request.recipientId, request.amount)
                        call.respond(ApiResponse("Money sent successfully", null))
                    } catch (e: Exception) {
                        call.respond(ApiResponse("Failed to send money: ${e.message}", null))
                    }
                }
            }

        }
    }
}

const val ISSUER = "sk_eepw_issuer_odas"
const val AUDIENCE = "sk_eepw_audience_odas"
const val SECRET = "sk_eepw_secret_odas"

fun generateToken(username: String): String {
    val tokenDuration = 60 * 6 * 1_000// 6 min

    val algorithm = Algorithm.HMAC256(SECRET)
    return JWT.create()
        .withSubject(username)
        .withIssuer(ISSUER)
        .withAudience(AUDIENCE)
        .withExpiresAt(Date(System.currentTimeMillis() + tokenDuration))
        .sign(algorithm)
}

fun makeJwtVerifier(): JWTVerifier {
    val algorithm = Algorithm.HMAC256(SECRET)
    return JWT.require(algorithm)
        .withIssuer(ISSUER)
        .withAudience(AUDIENCE)
        .build()
}

@Serializable
data class ApiResponse<T>( val message: String?, val data: T)

suspend inline fun ApplicationCall.respondBadRequest(slug: String? = null) {
    if (slug != null) {
        this.respond(HttpStatusCode.BadRequest, slug)
    } else {
        this.respond(HttpStatusCode.BadRequest)
    }
}


//@Serializer(forClass = HttpStatusCode::class)
//object HttpStatusCodeSerializer : KSerializer<HttpStatusCode> {
//
//    private val primitiveDescriptor: SerialDescriptor = PrimitiveSerialDescriptor("HttpStatusCode", PrimitiveKind.STRING)
//
//    override val descriptor: SerialDescriptor
//        get() = primitiveDescriptor
//
//
//    override fun deserialize(decoder: Decoder): HttpStatusCode {
//        val intValue = decoder.decodeInt()
//        return HttpStatusCode.fromValue(intValue)
//    }
//
//
//    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: HttpStatusCode) {
//        encoder.encodeString(value.value.toString())
//    }
//}

fun DatabaseUserModel.asSafeUser(): UserWithoutSecureData {
    return UserWithoutSecureData(
        this.name,
        this.email,
        this.id.toInt()
    )
}