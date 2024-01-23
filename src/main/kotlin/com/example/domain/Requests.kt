package com.example.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class DepositRequest(val userId: Int, val amount: Float)
data class WithdrawRequest(val userId: Int, val amount: Float)
data class SendMoneyRequest(val senderId: Int, val recipientId: Int, val amount: Float)

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
    @SerialName("surname")
    val surname: String,
    @SerialName("email")
    val email: String,
    @SerialName("password")
    val password: String,
)

@Serializable
data class LoginWithBitsRequest(val email: String, val bits: Map<Int, Char>)
