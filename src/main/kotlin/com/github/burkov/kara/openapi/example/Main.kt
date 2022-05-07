package com.github.burkov.kara.openapi.example

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.jackson.responseObject
import com.github.kittinunf.result.getOrNull
import io.swagger.v3.oas.models.OpenAPI
import kara.ApplicationConfig
import kara.server.JettyRunner
import kotlinx.reflection.Serialization
import kotlin.concurrent.thread

fun main() {
    runServer()
}

fun runServer() {
    Serialization.register(AnySerializer)
    val applicationConfig = ApplicationConfig(InMemoryConfigProvider, ApplicationConfig::class.java.classLoader!!)
    val runner = JettyRunner(applicationConfig)
    val tid = thread {
        test(runner)
    }
    runner.start()
    tid.join()
}

fun test(runner: JettyRunner) {
    Thread.sleep(1)

    "http://localhost:8080/openapi/schema/".httpGet().responseObject<OpenAPI> { request, response, result ->
        val openapi = result.getOrNull()!!
        runner.stop()
    }
}

