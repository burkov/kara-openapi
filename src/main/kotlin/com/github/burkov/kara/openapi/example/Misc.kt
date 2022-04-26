package com.github.burkov.kara.openapi.example

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotlinx.reflection.serialization.KClassSerializer
import kotlin.reflect.KClass

fun <T> tryOrNull(block: () -> T): T? {
    return try {
        block()
    } catch (e: Exception) {
        null
    }
}

private val objectMapper = ObjectMapper().registerKotlinModule()

object AnySerializer : KClassSerializer<Any> {
    override fun deserialize(param: String, paramType: KClass<*>): Any = objectMapper.readValue(param, paramType.java)
    override fun isThisType(testType: KClass<*>) = true
    override fun serialize(param: Any): String = objectMapper.writeValueAsString(param)
}