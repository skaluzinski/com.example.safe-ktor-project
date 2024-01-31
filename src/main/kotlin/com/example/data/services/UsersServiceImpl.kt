package com.example.data.services

import com.example.domain.Password
import com.example.core.HashingService
import com.example.data.database.UserDatabase
import com.example.domain.*
import kotlinx.datetime.*
import java.lang.Exception
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import javax.xml.crypto.Data

private const val TIMEZONE = "Europe/Berlin"

interface UsersService {
    fun isUserRegistered(email: Email): Boolean
    fun genLoginBitsIndexesOrThrow(email: Email): List<Int>
    fun isPasswordValidLoginCredential(email: Email, password: Password): Boolean
    fun getUserWithId(id: Int): DatabaseUserModel?
    fun getUserIdWithEmail(email: String): Int?
    fun getAll(): List<DatabaseUserModel>
    fun deleteUser(id: Int): Boolean
    fun getUserDataWithEmail(email: Email) : SafeUserModel?
    fun createUserOrFalse(model: NewUserModel): Boolean
    fun arePasswordBitesValidLoginCredentials(email: Email, passwordBitesWithIndex: Map<Int, Char>): Boolean
}

const val MAXIMUM_LENGTH_OF_LOGIN_BITS = 0.75
const val MINIMUM_LENGTH_OF_LOGIN_BITS = 0.40

private fun isPasswordValid(password: String): Boolean {
    val passwordPattern = Regex("^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[#?!@$%^&*-]).{8,}$")
    return password.matches(passwordPattern) && password.length <= 32
}

class UsersServiceImpl(
    private val usersDatabase: UserDatabase,
    private val hashingService: HashingService,
) : UsersService {

    private val loginBitsPossibleToLogin: ConcurrentMap<Email, LoginPasswordBits> = ConcurrentHashMap()

    override fun isUserRegistered(email: Email): Boolean {
        return usersDatabase.getUserOrNull(email.value) != null
    }

    override fun genLoginBitsIndexesOrThrow(email: Email): List<Int> {
        val userData = usersDatabase.getUserOrNull(email.value) ?: throw Exception(ErrorSlug.USER_NOT_FOUND.name)
        val passwordSize = userData.encryptedPasswordBites.size
        val allEncryptedPasswordCharacters = userData.encryptedPasswordBites

//        val maximumAmountOfLoginBits = floor(passwordSize.times(MAXIMUM_LENGTH_OF_LOGIN_BITS)).toInt()
//        val minimumAmountOfLoginBits = ceil(passwordSize.times(MINIMUM_LENGTH_OF_LOGIN_BITS)).toInt()

//        val possibleLoginBits = (minimumAmountOfLoginBits..maximumAmountOfLoginBits)
//        val amountOfChosenBits = possibleLoginBits.random()
        val amountOfChosenBits = 2
        val permittedIndexedLoginBits = mutableMapOf<Int, String>()

        while (permittedIndexedLoginBits.values.size != amountOfChosenBits) {
            val randomEncryptedCharacter = allEncryptedPasswordCharacters.random()

            if (!permittedIndexedLoginBits.containsValue(randomEncryptedCharacter)) {
                val index = allEncryptedPasswordCharacters.indexOf(randomEncryptedCharacter)
                permittedIndexedLoginBits[index] = randomEncryptedCharacter
            }
        }

        val timeZone = TimeZone.of(TIMEZONE)
        val localDateTime = Clock.System.now().toLocalDateTime(timeZone)
        val instant = localDateTime.toInstant(timeZone)

        val loginExpirationTime = instant.plus(15, DateTimeUnit.MINUTE, timeZone)

        val generatedLoginBitsCredentials = LoginPasswordBits(
            expiresAt = loginExpirationTime.toLocalDateTime(timeZone),
            hashedPasswordBits = permittedIndexedLoginBits
        )

        loginBitsPossibleToLogin[email] = generatedLoginBitsCredentials

        return permittedIndexedLoginBits.keys.toList()
    }

    override fun isPasswordValidLoginCredential(email: Email, password: Password): Boolean {
        val databaseUser = usersDatabase.getUserOrNull(email.value) ?: return false
        val hashedPassword = hashingService.hashString(string = password.value, salt = databaseUser.salt)

        return databaseUser.encryptedPassword == hashedPassword
    }

    override fun arePasswordBitesValidLoginCredentials(email: Email, passwordBitesWithIndex: Map<Int, Char>): Boolean {
        val userSalt = usersDatabase.getUserOrNull(email.value)?.salt ?: return false

        val permittedLoginCredentialsForUser = loginBitsPossibleToLogin[email] ?: return false
        val timeZone = TimeZone.of(TIMEZONE)
        val currentTime = Clock.System.now().toLocalDateTime(timeZone).toJavaLocalDateTime()

        val expiresAt = permittedLoginCredentialsForUser.expiresAt.toJavaLocalDateTime()
        val userCanLogin = expiresAt.isAfter(currentTime)

        if (!userCanLogin) {
            return false
        }

        val encryptedBits =
            passwordBitesWithIndex.mapValues { hashingService.hashString(string = it.toString(), salt = userSalt) }
        encryptedBits.forEach { t, u ->
            println("$t $u")
        }

        usersDatabase.getUserEmailBits(email = email.value).forEachIndexed { index, c ->
            println("### $index : $c")
        }
        val bits = usersDatabase.getUserEmailBits(email = email.value)
        val knownBits = permittedLoginCredentialsForUser.hashedPasswordBits
        val areBitsCorrect = encryptedBits.all { (index, key) ->
            knownBits[index] == key
        }

        return areBitsCorrect
    }

    override fun createUserOrFalse(model: NewUserModel): Boolean {
        val salt = hashingService.generateSalt()
        if (!isPasswordValid(model.password)) {
            return false
        }

        if (usersDatabase.userWithEmailOrNull(email = model.email) != null) {
            return false
        }

        val encryptedPassword = hashingService.hashString(model.password, salt)
        var encryptedPasswordBites = ""
        model.password.map { letter ->
            hashingService.hashString(letter.toString(), salt)
        }.forEachIndexed { index, s ->
            if (index != 0) {
                encryptedPasswordBites += ","
            }
            encryptedPasswordBites += s
        }

        if (encryptedPasswordBites.isBlank()) {
            return false
        }

        val oldSize = usersDatabase.getUsers().size

        val newUser = GeneralUserModel(
            name = model.name,
            email = model.email,
            password = encryptedPassword,
            joinedPasswordBites = encryptedPasswordBites,
            balance = 0f
        )

        usersDatabase.createUser(newUser, salt)
        val newSize = usersDatabase.getUsers().size

        return newSize != oldSize
    }

    override fun getUserWithId(id: Int): DatabaseUserModel? {
        return usersDatabase.getUserByIdOrNull(id)
    }

    override fun getUserIdWithEmail(email: String): Int? {
        return usersDatabase.getUserOrNull(email)?.id?.toInt()
    }

    override fun getAll(): List<DatabaseUserModel> {
        return usersDatabase.getUsers()
    }

    override fun deleteUser(id: Int): Boolean {
        return usersDatabase.deleteUser(id)
    }

    override fun getUserDataWithEmail(email: Email): SafeUserModel? {
        return usersDatabase.getUserByEmailOrNull(email.value)?.asSafeModel()
    }
}
