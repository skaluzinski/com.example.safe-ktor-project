package com.example.plugins

import com.example.module
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.testing.*
import kotlin.test.Test

class RoutingKtTest {

    @Test
    fun testGetArticles() = testApplication {
        application {
            module()
        }
        client.get("/articles").apply {
            println("### ${this.call.response.bodyAsText()}")
        }
    }


    @Test
    fun testPostCreateuser() = testApplication {
        application {
//            configureRouting()
        }
        client.post("/create_user").apply {
            TODO("Please write your test here")
        }
    }
}