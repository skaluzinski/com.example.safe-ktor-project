package com.example.plugins

import arrow.fx.coroutines.Use
import com.example.domain.ApiResponse
import com.example.domain.ErrorSlug
import com.example.domain.ErrorSlugException
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlin.math.min

suspend fun PipelineContext<*, ApplicationCall>.respondWithErrorSlug(error: ErrorSlug) {
    this.call.respond(ApiResponse("SLUG:[${error.name}]", null))
}

suspend fun PipelineContext<*, ApplicationCall>.respondWithErrorSlugs(errors: List<ErrorSlug>) {
    this.call.respond(ApiResponse("SLUG:[${errors.map { it.name }.joinToString { "," }}]", null))
}

suspend inline fun <reified T: Any> PipelineContext<*, ApplicationCall>.respondWithData(data: T) {
    this.call.respond(ApiResponse(null, data))
}

fun PipelineContext<*, ApplicationCall>.getCurrentUserSessionOrThrow(): UserSession {
    return this.call.sessions.get<UserSession>() ?: throw ErrorSlugException(ErrorSlug.OPERATION_NEEDS_RECENT_LOGIN)
}

suspend fun PipelineContext<*, ApplicationCall>.respondWithOperationSuccess(success: Boolean) {
    this.call.respond(ApiResponse(null, mapOf("operation_success" to success )))
}

private fun randomIntBetween(from: Int, to: Int) = (Math.random() * (to - from) + from).toInt()


suspend fun takeAtLeastTimeBetween(
    minInMillis: Long = 800,
    maxInMillis: Long = 1700,
    startReferenceTimeInMillis: Long,
    action: suspend () -> Unit,
) {
    val expectedExecutionTime = startReferenceTimeInMillis + randomIntBetween(minInMillis.toInt(), maxInMillis.toInt())

    val checkTimeInMillis = System.currentTimeMillis()
    val timeUntilEnd = expectedExecutionTime - checkTimeInMillis

    if (timeUntilEnd > 0) {
        delay(timeUntilEnd)
    }

    action()
}