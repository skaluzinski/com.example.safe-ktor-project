package com.example.domain

class ErrorSlugs {
    companion object {
        const val PASSWORD_LENGTH_TOO_SHORT = "PASSWORD_LENGTH_TOO_SHORT"
        const val PASSWORD_LENGTH_TOO_LONG = "PASSWORD_LENGTH_TOO_LONG"
        const val PASSWORD_MISSING_DIGIT = "PASSWORD_MISSING_DIGIT"
        const val PASSWORD_MISSING_LOWERCASE = "PASSWORD_MISSING_LOWERCASE"
        const val PASSWORD_MISSING_UPPERCASE = "PASSWORD_MISSING_UPPERCASE"
        const val PASSWORD_CONTAINS_INVALID_CHARACTERS = "PASSWORD_CONTAINS_INVALID_CHARACTERS"
        const val INVALID_PASSWORD_BIT = "INVALID_PASSWORD_BIT"
        const val USER_NOT_FOUND = "USER_NOT_FOUND"
        const val INVALID_LOGIN_CREDENTIALS = "INVALID_LOGIN_CREDENTIALS"
        const val EMAIL_INVALID = "EMAIL_INVALID"
        const val BALANCE_NOT_SUFICIENT = "BALANCE_NOT_SUFICIENT"
    }
}