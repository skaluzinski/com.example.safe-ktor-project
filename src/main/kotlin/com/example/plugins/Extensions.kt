package com.example.plugins

import arrow.fx.coroutines.Use
import com.example.domain.ApiResponse
import com.example.domain.ErrorSlug
import com.example.domain.ErrorSlugException
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import io.ktor.util.pipeline.*

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
    this.call.respond(ApiResponse(null, mapOf("operation_state" to success )))
}