package com.example.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class EmailException(override val message: String) : CredentialException(message)

@Serializable
data class Email(
    @SerialName("email")
    val value: String,
) {
    init {
        require(value.matches(Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"))) {
            EmailException(
                ErrorSlugs.EMAIL_INVALID
            )
        }
    }
}