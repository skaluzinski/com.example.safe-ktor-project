package com.example.domain

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