package com.example.data.database

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.Database
import com.example.Users
import com.example.data.services.NewTransaction
import com.example.data.services.NewTwoWayTransaction
import com.example.data.services.asDatabaseTransaction
import com.example.domain.DatabaseUserModel
import com.example.domain.GeneralUserModel
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.lang.Exception
import java.security.Key
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

private const val DATABASE_URL = "jdbc:sqlite:test133237.db"

private val transactionsAdapter  = object : ColumnAdapter<List<DatabaseTransaction>, String>  {
    override fun decode(databaseValue: String): List<DatabaseTransaction> {

        return if (databaseValue.isEmpty()) {
            emptyList()
        } else {
            databaseValue.split(";").map { Json.decodeFromString(it) }
        }
    }

    override fun encode(value: List<DatabaseTransaction>): String {
        return value.joinToString(";") { Json.encodeToString(it) }
    }

}


class UserDatabase {
    private var _database: Database

    init {
        Class.forName("org.sqlite.JDBC").newInstance();

        val driver: SqlDriver = JdbcSqliteDriver(
            url = DATABASE_URL,
            schema = Database.Schema,
            properties = Properties().apply { put("foreign_keys", "true") }
        )
        Database.Schema.create(driver)
        _database = Database.invoke(
            driver = driver,
            usersAdapter = Users.Adapter(
                transactionsAdapter = transactionsAdapter
            )
        )
    }

    fun getUserOrNull(email: String): DatabaseUserModel? {
        return try {
            val queryResult = _database.userQueriesQueries.selectUserByEmail(email).executeAsOne()
            queryResult.asDatabaseModel()
        } catch (e: Exception) {
            null
        }
    }

    fun getUserByIdOrNull(id: Int): DatabaseUserModel? {
        return try {
            val queryResult = _database.userQueriesQueries.selectUserByID(id.toLong()).executeAsOne()
            queryResult.asDatabaseModel()
        } catch (e: Exception) {
            null
        }
    }

    fun getUserByEmailOrNull(email: String): DatabaseUserModel? {
        return try {
            val queryResult = _database.userQueriesQueries.selectUserByEmail(email).executeAsOne()
            queryResult.asDatabaseModel()
        } catch (e: Exception) {
            null
        }
    }

    fun getUsers(): List<DatabaseUserModel> {
        return _database.userQueriesQueries.selectAll().executeAsList().map {
            it.asDatabaseModel()
        }
    }

    fun getUserEmailBits(email: String): List<String> {
        return _database.userQueriesQueries.selectUserByEmail(email).executeAsOne().password_bites.split(",")
    }

    fun userWithEmailOrNull(email: String): DatabaseUserModel? {
        return _database.userQueriesQueries.selectUserByEmail(email).executeAsOneOrNull()?.asDatabaseModel()
    }

    fun createUser(user: GeneralUserModel, salt: String) {
        val cardNumber = generateRandomCardNumber()
        val encryptedCardNumber = encrypt(cardNumber, user.password)

        _database.userQueriesQueries.createUser(
            name = user.name,
            email = user.email,
            card_number = encryptedCardNumber,
            entire_password = user.password,
            password_bites = user.joinedPasswordBites!!,
            salt = salt,
            balance = 0,
        )
    }

    fun deleteUser(id: Int): Boolean {
        _database.userQueriesQueries.deleteUser(id.toLong())
        val queryResultAfterDeletion = _database.userQueriesQueries.selectUserByID(id.toLong()).executeAsOneOrNull()
        return queryResultAfterDeletion == null
    }

    fun updateUserBalance(email: String, newBalance: Float) {
        val revisedBalance = newBalance.times(100).toInt().toLong()

        _database.userQueriesQueries.updateUserBalanceWithEmail(balance = revisedBalance, email = email)
    }

    fun addIndependentTransaction(newTransaction: NewTransaction) {
        val user = _database.userQueriesQueries.selectUserByEmail(newTransaction.senderEmail).executeAsOne()
        var transactions = user.transactions?.toMutableList()

        if (transactions != null) {
            transactions.add(newTransaction.asDatabaseTransaction((transactions.lastIndex.plus(1)).toLong()))
        } else {
            transactions = mutableListOf(newTransaction.asDatabaseTransaction(0))
        }
        _database.userQueriesQueries.updateTransactionsForUser(
            transactions = transactions, email = newTransaction.senderEmail
        )
    }

    fun addTwoWayTransaction(newTransaction: NewTwoWayTransaction) {
        val sender =_database.userQueriesQueries.selectUserByEmail(newTransaction.senderEmail).executeAsOne()
        val senderTransactions = sender.transactions?.toMutableList() ?: mutableListOf()

        val senderNewTransactionId = if (senderTransactions.isEmpty()) {
            0
        } else {
            senderTransactions.lastIndex.plus(1)
        }

        val senderTransaction = NewTransaction(
            recipientEmail = newTransaction.recipientEmail,
            balanceBefore = newTransaction.senderBalanceBefore,
            balanceAfter = newTransaction.senderBalanceAfter,
            senderEmail = newTransaction.senderEmail,
            transactionDate = LocalDate.now(),
            transactionAmount = newTransaction.transactionAmount,
            title = newTransaction.title,
            type = "Paid"
        )

        senderTransactions.add(senderTransaction.asDatabaseTransaction(senderNewTransactionId.toLong()))

        val recipient = _database.userQueriesQueries.selectUserByEmail(newTransaction.recipientEmail!!).executeAsOne()
        val recipientTransactions = recipient.transactions?.toMutableList() ?: mutableListOf()

        val recipientTransaction = NewTransaction(
            recipientEmail = newTransaction.recipientEmail,
            balanceBefore = newTransaction.recipientBalanceBefore,
            balanceAfter = newTransaction.recipientBalanceAfter,
            senderEmail = newTransaction.senderEmail,
            transactionDate = LocalDate.now(),
            transactionAmount = newTransaction.transactionAmount,
            title = newTransaction.title,
            type = "Received"
        )

        val recipientNewTransactionId = if (recipientTransactions.isEmpty()) {
            0
        } else {
            recipientTransactions.lastIndex.plus(1)
        }

        recipientTransactions.add(recipientTransaction.asDatabaseTransaction(recipientNewTransactionId.toLong()))

        _database.userQueriesQueries.updateTransactionsForUser(
            transactions = senderTransactions,
            email = sender.email
        )
        _database.userQueriesQueries.updateTransactionsForUser(
            transactions = recipientTransactions,
            email = recipient.email
        )
    }
}


private fun generateSecretKey(userKeyPart: String): Key {
    val masterKeyPartInBytes = System.getenv("encryptMasterKey").toByteArray().take(24)
    val userKeyParInBytes = userKeyPart.toByteArray().take(8)
    val keyBytes = mutableListOf<Byte>()
    keyBytes.addAll(masterKeyPartInBytes)
    keyBytes.addAll(userKeyParInBytes)

    return SecretKeySpec(keyBytes.toByteArray(), "AES")
}

private fun encrypt(value: String, userKeyPart: String): String {
    val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
    val secretKey = generateSecretKey(userKeyPart)
    cipher.init(Cipher.ENCRYPT_MODE, secretKey)

    // Assuming UTF-8 encoding for simplicity
    val encryptedBytes = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
    return Base64.getEncoder().encodeToString(encryptedBytes)
}

private fun decrypt(value: String,userKeyPart: String): String {
    val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
    val secretKey = generateSecretKey(userKeyPart)

    cipher.init(Cipher.DECRYPT_MODE, secretKey)

    val encryptedBytes = Base64.getDecoder().decode(value)
    val decryptedBytes = cipher.doFinal(encryptedBytes)
    return String(decryptedBytes, Charsets.UTF_8)
}

fun Users.asDatabaseModel(): DatabaseUserModel {
    return DatabaseUserModel(
        id = this.id,
        name = this.name,
        email = this.email,
        cardNumber = decrypt(card_number, this.entire_password),
        encryptedPassword = this.entire_password,
        encryptedPasswordBites = this.password_bites.split(",").filter { it != "," },
        balance = this.balance.div(100f),
        salt = this.salt,
        transactions = this.transactions?.map { it.asRevisedTransaction() } ?: emptyList()
    )
}

@Serializable
data class DatabaseTransaction(
    val transactionId: Long,
    @Serializable(with = LocalDateSerializer::class)
    val transactionDate: LocalDate,
    val senderEmail: String,
    val title: String,
    val type: String,
    val recipientEmail: String?,
    val balanceBefore: Int,
    val balanceAfter: Int,
    val transactionAmount: Int
)

@Serializable
data class RevisedTransaction(
    val transactionId: Long,
    @Serializable(with = LocalDateSerializer::class)
    val transactionDate: LocalDate,
    val senderEmail: String,
    val recipientEmail: String?,
    val balanceBefore: Float,
    val balanceAfter: Float,
    val type: String,
    val title: String,
    val transactionAmount: Float,
)

//fun RevisedTransaction.asDatabaseTransaction(): DatabaseTransaction {
//    return DatabaseTransaction(
//        transactionId = transactionId,
//        transactionDate = transactionDate,
//        senderEmail = senderEmail,
//        recipientEmail = recipientEmail,
//        balanceBefore = balanceBefore.times(100).toInt(),
//        balanceAfter = balanceAfter.times(100).toInt(),
//        transactionAmount = transactionAmount.times(100).toInt(),
//        type = this
//    )
//}
fun DatabaseTransaction.asRevisedTransaction(): RevisedTransaction {
    return RevisedTransaction(
        transactionId = transactionId,
        transactionDate = transactionDate,
        senderEmail = senderEmail,
        recipientEmail = recipientEmail,
        balanceAfter = balanceAfter.div(100f),
        balanceBefore = balanceBefore.div(100f),
        transactionAmount = transactionAmount.div(100f),
        type = type,
        title = title
    )
}

object LocalDateSerializer : KSerializer<LocalDate> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDate) {
        val result = value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        encoder.encodeString(result)
    }

    override fun deserialize(decoder: Decoder): LocalDate {
        return LocalDate.parse(decoder.decodeString())
    }
}

private fun generateRandomCardNumber(): String {
    val random = Random()
    val builder = StringBuilder("4")
    repeat(3) {
        builder.append(random.nextInt(10))
    }
    repeat(3) {
        builder.append("-")
        repeat(4) {
            builder.append(random.nextInt(10))
        }
    }

    return builder.toString()
}
