@file:Suppress("UNCHECKED_CAST")

package com.github.burkov.kara.openapi.example

import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.jackson.responseObject
import io.swagger.v3.core.util.Json
import io.swagger.v3.oas.models.OpenAPI
import kara.ApplicationConfig
import kara.server.JettyRunner
import kotlinx.reflection.Serialization
import kotlinx.reflection.serialization.KClassSerializer
import tanvd.konfy.provider.ConfigProvider
import java.lang.reflect.Type
import kotlin.concurrent.thread
import kotlin.reflect.KClass

fun main() {
    runServer()
}


fun runServer() {
    Serialization.register(AnySerializer)
    val applicationConfig = ApplicationConfig(InMemoryConfigProvider, ApplicationConfig::class.java.classLoader!!)
    val runner = JettyRunner(applicationConfig)
//    thread { test(runner) }.also {
        runner.start()
//    }.join()
}


private object AnySerializer : KClassSerializer<Any> {
    private val mapper = Json.mapper().registerKotlinModule()
    override fun deserialize(param: String, paramType: KClass<*>): Any = mapper.readValue(param, paramType.java)
    override fun isThisType(testType: KClass<*>) = true
    override fun serialize(param: Any): String = mapper.writeValueAsString(param)
}

private object InMemoryConfigProvider : ConfigProvider() {
    override fun <T : Any> fetch(key: String, type: Type): T? {
        return when (key) {
            "kara.routePackages" -> arrayOf("com.github.burkov.kara.openapi") as T
            "kara.environment" -> "development" as T
            "kara.port" -> "8080" as T
            else -> null
        }
    }
}

private fun test(runner: JettyRunner) {
//    "http://localhost:8080/openapi/com.github.burkov.kara.openapi.example.EmailsController".httpGet()
//        .responseString { _, _, b ->
//            println(b.get())
//            runner.stop()
//        }
    "http://localhost:8080/openapi/com.github.burkov.kara.openapi.example.UsersController".httpGet()
        .responseString { _, _, b ->
            println(b.get())
            runner.stop()
        }
}

