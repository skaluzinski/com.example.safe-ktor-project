package com.example.domain

@JvmInline
value class Password(val value: String) {
    init {
        val invalidLetters = Regex("[;\\-,'\"]")

        require(value.length >= 8) { throw ErrorSlugException(ErrorSlug.PASSWORD_LENGTH_TOO_SHORT) }
        require(value.length <= 32) { throw ErrorSlugException(ErrorSlug.PASSWORD_LENGTH_TOO_LONG) }
        require(value.any { it.isDigit() }) { throw ErrorSlugException(ErrorSlug.PASSWORD_MISSING_DIGIT) }
        require(value.any { it.isLowerCase() }) { throw ErrorSlugException(ErrorSlug.PASSWORD_MISSING_LOWERCASE) }
        require(value.any { it.isUpperCase() }) { throw ErrorSlugException(ErrorSlug.PASSWORD_MISSING_UPPERCASE) }
        require(!value.matches(invalidLetters)) { throw ErrorSlugException(ErrorSlug.PASSWORD_CONTAINS_INVALID_CHARACTERS) }
    }
}

@JvmInline
value class PasswordBit(val character: Char) {
    init {
        val invalidLetters = Regex("[;\\-,'\"]")

        require(
            character.isDigit() ||
                    character.isLowerCase() ||
                    character.isUpperCase() ||
                    !invalidLetters.matches(character.toString())
        ) { throw ErrorSlugException(ErrorSlug.INVALID_PASSWORD_BIT) }
    }
}
