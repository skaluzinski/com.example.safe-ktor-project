package com.example.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DepositRequest(val amount: Float)

@Serializable
data class WithdrawRequest(val amount: Float)

@Serializable
data class SendMoneyRequest(val recipientEmail: String, val title: String, val amount: Float)

@Serializable
data class EmailRequest(
    @SerialName("email")
    val email: String,
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    @SerialName("name")
    val name: String,
    @SerialName("email")
    val email: String,
    @SerialName("password")
    val password: String,
)


@Serializable
data class LoginWithBitsRequest(val email: String, val bits: Map<Int, Char>)
