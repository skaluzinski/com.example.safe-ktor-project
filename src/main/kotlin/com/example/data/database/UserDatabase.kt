package com.example.data.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.Database
import com.example.Users
import com.example.data.services.DatabaseUserModel
import com.example.domain.GeneralUserModel
import java.lang.Exception

private const val DATABASE_URL = "jdbc:sqlite:test13237.db"

class UserDatabase {
    private var _database: Database

    init {
        val driver: SqlDriver = JdbcSqliteDriver(DATABASE_URL, schema = Database.Schema)
        Database.Schema.create(driver)
        _database = Database.invoke(driver)
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
        _database.userQueriesQueries.createUser(
            name = user.name,
            email = user.email,
            entire_password = user.password,
            password_bites = user.joinedPasswordBites!!,
            salt = salt,
            balance = 0.0
        )
    }

    fun deleteUser(id: Int): Boolean {
        _database.userQueriesQueries.deleteUser(id.toLong())
        val queryResultAfterDeletion = _database.userQueriesQueries.selectUserByID(id.toLong()).executeAsOneOrNull()
        return queryResultAfterDeletion == null
    }

    fun updateUserBalance(userId: Int, newBalance: Double) {
        _database.userQueriesQueries.updateUserBalance(newBalance, userId.toLong())
    }
}

fun Users.asDatabaseModel(): DatabaseUserModel {
    return DatabaseUserModel(
        id = this.id,
        name = this.name,
        email = this.email,
        encryptedPassword = this.entire_password,
        encryptedPasswordBites = this.password_bites.split(",").filter { it != "," },
        balance = this.balance.toFloat(),
        salt = this.salt
    )
}