package com.example.data.services

import com.example.data.database.UserDatabase
import com.example.domain.ErrorSlugs

interface TransactionService {
    fun sendMoneyOrThrow(senderId: Int, recipientId: Int, amount: Float)
    fun depositMoneyOrThrow(userId: Int, amount: Float)
    fun withdrawMoneyOrThrow(userId: Int, amount: Float)
}

class TransactionServiceImpl(
    private val usersDatabase: UserDatabase,
) : TransactionService {
    override fun depositMoneyOrThrow(userId: Int, amount: Float) {
        val user = usersDatabase.getUserByIdOrNull(userId)
            ?: throw java.lang.IllegalArgumentException(ErrorSlugs.USER_NOT_FOUND)

        val updatedBalance = user.balance + amount
        usersDatabase.updateUserBalance(userId, updatedBalance.toDouble())
    }

    override fun withdrawMoneyOrThrow(userId: Int, amount: Float) {
        val user = usersDatabase.getUserByIdOrNull(userId)
            ?: throw java.lang.IllegalArgumentException(ErrorSlugs.USER_NOT_FOUND)

        if (user.balance < amount) {
            throw IllegalArgumentException(ErrorSlugs.BALANCE_NOT_SUFICIENT)
        }

        val updatedBalance = user.balance - amount
        usersDatabase.updateUserBalance(userId, updatedBalance.toDouble())
    }

    override fun sendMoneyOrThrow(senderId: Int, recipientId: Int, amount: Float) {
        val sender = usersDatabase.getUserByIdOrNull(senderId)
        val recipient = usersDatabase.getUserByIdOrNull(recipientId)

        if (sender == null || recipient == null) {
            throw java.lang.IllegalArgumentException(ErrorSlugs.USER_NOT_FOUND)
        }

        if (sender.balance < amount) {
            throw IllegalArgumentException(ErrorSlugs.BALANCE_NOT_SUFICIENT)
        }

        val updatedSenderBalance = sender.balance - amount
        val updatedRecipientBalance = recipient.balance + amount

        usersDatabase.updateUserBalance(senderId, updatedSenderBalance.toDouble())
        usersDatabase.updateUserBalance(recipientId, updatedRecipientBalance.toDouble())
    }
}