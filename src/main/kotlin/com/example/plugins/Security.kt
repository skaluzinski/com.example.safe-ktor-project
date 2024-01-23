package com.example.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import java.security.MessageDigest
import java.util.*
import kotlin.text.Charsets.UTF_8

data class CustomCredentials(val username: String, val customField: String)

data class CustomPrincipal(val userName: String, val realm: String) : Principal


fun Application.configureSecurity() {
    install(Authentication) {
        jwt("jwt") {
            verifier(makeJwtVerifier())
            validate {
                if (it.payload.expiresAt?.after(Date()) == true) {
                    JWTPrincipal(it.payload)
                } else {
                    null
                }
            }
        }
    }
}
