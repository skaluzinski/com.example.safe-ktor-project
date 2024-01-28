package com.example.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Email(
    @SerialName("email")
    val value: String,
) {
    init {
        require(value.matches(Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"))) {
            ErrorSlugException(ErrorSlug.EMAIL_INVALID)
        }
    }
}