package com.example.domain

import com.example.data.database.RevisedTransaction
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class NewUserModel(
    val name: String,
    val email: String,
    val password: String,
)

@Serializable
data class LoginUserModel(
    val email: String?,
    val password: String,
)

data class GeneralUserModel(
    val name: String,
    val email:String,
    val password: String,
    val joinedPasswordBites: String? = null,
    val balance: Float
)

@Serializable
data class UserWithoutSecureData(
    val name: String,
    val email: String,
    val id: Int
)

@Serializable
data class PrivateUserModel(
    val name: String,
    val email: String,
    val balance: Float,
    val accountNumber: String
)


data class DatabaseUserModel(
    val id: Long,
    val name: String,
    val email: String,
    val encryptedPassword: String,
    val encryptedPasswordBites: List<String>,
    val salt: String,
    val balance: Float,
    val transactions: List<RevisedTransaction>
)

@Serializable
data class LoginPasswordBits(
    val expiresAt: LocalDateTime,
    val hashedPasswordBits: Map<Int, String>,
)

@Serializable
data class SafeUserModel(
    val name: String,
    val email: String,
    val balance: Float,
    val transactions: List<RevisedTransaction>
)

fun DatabaseUserModel.asSafeModel(): SafeUserModel {
    return SafeUserModel(this.name, this.email, this.balance, transactions)
}