package com.example.plugins

import com.example.module
import io.ktor.http.ContentDisposition.Companion.File
import io.ktor.network.tls.certificates.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import org.slf4j.LoggerFactory
import java.io.File

fun TLSEnviroment(): ApplicationEngineEnvironment {
    val keyStoreFile = File("build/keystore.jks")
    val keyStore = buildKeyStore {
        certificate("sampleSebastianAlias") {
            password = "sample123"
            domains = listOf("127.0.0.1", "0.0.0.0", "localhost")
        }
    }
    keyStore.saveToFile(keyStoreFile, "qwerty1234")

    val environment = applicationEngineEnvironment {
        log = LoggerFactory.getLogger("ktor.application")
        connector {
            port = 8080
        }
//        sslConnector(
//            keyStore = keyStore,
//            keyAlias = "sampleSebastianAlias",
//            keyStorePassword = { "qwerty1234".toCharArray() },
//            privateKeyPassword = { "sample123".toCharArray() }) {
//            port = 8443
//            keyStorePath = keyStoreFile
//        }
        module(Application::module)
    }

    return environment
}