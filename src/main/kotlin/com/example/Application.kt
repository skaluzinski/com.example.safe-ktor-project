package com.example

import com.example.core.HashingServiceImpl
import com.example.data.database.UserDatabase
import com.example.data.services.TransactionServiceImpl
import com.example.data.services.UsersServiceImpl
import com.example.plugins.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    val tlsEnvironment = TLSEnviroment()

    embeddedServer(
        factory = Netty,
        environment = tlsEnvironment
    ).start(wait = true)
}

fun Application.module() {
    val userDatabase = UserDatabase()
    val hashingService = HashingServiceImpl()
    val transactionService = TransactionServiceImpl(usersDatabase = userDatabase)
    val usersService = UsersServiceImpl(usersDatabase = userDatabase, hashingService = hashingService)

    configureSecurity()
    configureSerialization()
    configureMonitoring()
    configureHTTP()
    configureRouting(
        usersService = usersService,
        transactionService = transactionService
    )
}


