package com.example.data.services

import com.example.data.database.DatabaseTransaction
import com.example.data.database.LocalDateSerializer
import com.example.data.database.RevisedTransaction
import com.example.data.database.UserDatabase
import com.example.domain.Email
import com.example.domain.ErrorSlug
import com.example.domain.ErrorSlugException
import kotlinx.serialization.Serializable
import java.time.LocalDate

interface TransactionService {
    fun depositMoneyOrThrow(email: Email, amount: Float)
    fun sendMoneyOrThrow(senderEmail: Email, recipentEmail: Email, amount: Float)
    fun withdrawMoneyOrThrow(userEmail: Email, amount: Float)
}

class TransactionServiceImpl(
    private val usersDatabase: UserDatabase,
) : TransactionService {
    override fun depositMoneyOrThrow(email: Email, amount: Float) {
        val user = usersDatabase.getUserByEmailOrNull(email.value) ?: throw ErrorSlugException(ErrorSlug.USER_NOT_FOUND)

        val updatedBalance = user.balance + amount
        usersDatabase.addIndependentTransaction(
            newTransaction = NewTransaction(
                transactionDate = LocalDate.now(),
                senderEmail = user.email,
                transactionAmount = amount,
                balanceAfter = user.balance + amount,
                balanceBefore = user.balance,
                recipientEmail = null,
                title = "Bankomat 21137",
                type = "Depozyt"
            )
        )

        usersDatabase.updateUserBalance(email.value, updatedBalance)
    }

    override fun withdrawMoneyOrThrow(userEmail: Email, amount: Float) {
        val user = usersDatabase.getUserByEmailOrNull(userEmail.value) ?: throw ErrorSlugException(ErrorSlug.USER_NOT_FOUND)

        if (user.balance < amount) {
            throw ErrorSlugException(ErrorSlug.BALANCE_NOT_SUFFICIENT)
        }

        val updatedBalance = user.balance - amount
        usersDatabase.addIndependentTransaction(
            newTransaction = NewTransaction(
                transactionDate = LocalDate.now(),
                senderEmail = user.email,
                transactionAmount = amount,
                balanceAfter = user.balance - amount,
                balanceBefore = user.balance,
                recipientEmail = null,
                title = "Bankomat 21137",
                type = "Wypłata"
            )
        )

        usersDatabase.updateUserBalance(userEmail.value, updatedBalance)
    }

    override fun sendMoneyOrThrow(senderEmail: Email, recipentEmail: Email, amount: Float) {
        val sender = usersDatabase.getUserByEmailOrNull(senderEmail.value) ?: throw ErrorSlugException(ErrorSlug.INVALID_SENDER_CREDENTIALS)
        val recipient = usersDatabase.getUserByEmailOrNull(recipentEmail.value) ?: throw ErrorSlugException(ErrorSlug.INVALID_RECIPIENT_CREDENTIALS)

        if (sender.balance < amount) {
            throw ErrorSlugException(ErrorSlug.BALANCE_NOT_SUFFICIENT)
        }

        val updatedSenderBalance = sender.balance - amount
        val updatedRecipientBalance = recipient.balance + amount

        usersDatabase.updateUserBalance(senderEmail.value, updatedSenderBalance)
        usersDatabase.updateUserBalance(recipentEmail.value, updatedRecipientBalance)
    }

    private fun addWithDrawTransaction(email: Email, amount: Float) {
        val user = usersDatabase.getUserByEmailOrNull(email.value) ?: throw ErrorSlugException(ErrorSlug.INVALID_SENDER_CREDENTIALS)
    }
}

@Serializable
data class NewTransaction(
    @Serializable(with = LocalDateSerializer::class)
    val transactionDate: LocalDate,
    val senderEmail: String,
    val recipientEmail: String?,
    val balanceBefore: Float,
    val type: String,
    val title: String,
    val balanceAfter: Float,
    val transactionAmount: Float,
)

fun NewTransaction.asDatabaseTransaction(transactionId: Long): DatabaseTransaction {
    return DatabaseTransaction(
        transactionId = transactionId,
        transactionDate = transactionDate,
        senderEmail = senderEmail,
        recipientEmail = recipientEmail,
        balanceBefore = balanceBefore.times(100).toInt(),
        balanceAfter = balanceAfter.times(100).toInt(),
        transactionAmount = transactionAmount.times(100).toInt(),
        title = title,
        type = type
    )
}