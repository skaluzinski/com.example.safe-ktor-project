package com.example.plugins

import com.example.domain.Email
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import io.ktor.server.sessions.serialization.*
import io.ktor.util.*
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.json.Json
import java.util.UUID

fun Application.configureSecurity() {
    install(Sessions) {
        val secretSignKey = hex("6819b57a326945c1968f45236589")
        cookie<UserSession>("user_session", SessionStorageMemory()) {
            serializer = KotlinxSessionSerializer(Json)
            cookie.path = "/"
            transform(SessionTransportTransformerMessageAuthentication(secretSignKey))
        }
    }
    install(Authentication) {
        session<UserSession>("user_session") {
            validate { session ->
                session.takeIf { it.isSessionStillValid() }
            }
            challenge {
                this.call.respond(UnauthorizedResponse())
            }
        }
    }
}

private const val SESSION_LENGTH_IN_MINUTES = 15

@Serializable
data class UserSession(
    @Serializable(with = UUIDSerializer::class)
    val uuid: UUID,
    val email: Email,
    val count: Int = 0,
    val expireTime: LocalDateTime,
) : Principal

fun UserSession.isSessionStillValid(): Boolean {
    val currentTime = java.time.LocalDateTime.now()
    return currentTime.isAfter(this.expireTime.toJavaLocalDateTime())
}

object UUIDSerializer : KSerializer<UUID> {
    override val descriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): UUID {
        return UUID.fromString(decoder.decodeString())
    }

    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: UUID) {
        encoder.encodeString(value.toString())
    }
}
