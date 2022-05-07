package com.github.burkov.kara.openapi.example

import io.swagger.v3.core.util.Json
import kotlinx.reflection.serialization.KClassSerializer
import kotlin.reflect.KClass

fun <T> tryOrNull(block: () -> T): T? {
    return try {
        block()
    } catch (e: Exception) {
        null
    }
}

val om = Json.mapper()

object AnySerializer : KClassSerializer<Any> {
    override fun deserialize(param: String, paramType: KClass<*>): Any = om.readValue(param, paramType.java)
    override fun isThisType(testType: KClass<*>) = true
    override fun serialize(param: Any): String = om.writeValueAsString(param)
}