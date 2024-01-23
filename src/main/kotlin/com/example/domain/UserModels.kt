package com.example.domain

import kotlinx.serialization.Serializable

@Serializable
data class NewUserModel(
    val name: String,
    val surname: String,
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
    val surname: String,
    val email:String,
    val password: String,
    val joinedPasswordBites: String? = null,
    val balance: Float
)

data class UserWithoutSecureData(
    val name: String,
    val email: String,
    val id: Int
)