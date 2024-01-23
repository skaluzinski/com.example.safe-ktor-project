package com.example.core

import java.security.MessageDigest
import java.security.SecureRandom

interface HashingService {
    fun generateSalt() : String
    fun hashString(string: String, salt: String) : String
    fun hashListOfChars(charList: List<Char>): String
}

class HashingServiceImpl: HashingService {
    override fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return salt.toString()
    }

    override fun hashString(string: String, salt: String): String {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val passwordWithSalt = (string+ salt).toByteArray()
        val hashedPassword = messageDigest.digest(passwordWithSalt)
        return hashedPassword.joinToString("") { "%02x".format(it) }
    }

    override fun hashListOfChars(charList: List<Char>): String {
        val charArray = charList.toCharArray()
        val charString = String(charArray)

        val messageDigest = MessageDigest.getInstance("SHA-256")
        val hashedBytes = messageDigest.digest(charString.toByteArray())

        return hashedBytes.joinToString("") { "%02x".format(it) }
    }
}