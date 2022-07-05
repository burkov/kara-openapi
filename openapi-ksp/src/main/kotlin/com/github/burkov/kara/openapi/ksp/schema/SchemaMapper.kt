package com.github.burkov.kara.openapi.ksp.schema

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType

class SchemaMapper {
    private val resolvedTypes = mutableMapOf<String, KType>()

    private fun resolvedRef(type: KType): String {
        val kClass = type.classifier as KClass<*>
        val name = kClass.simpleName
        requireNotNull(name) { "Type '$type' has no simple name" }
        if (resolvedTypes[name] != null && resolvedTypes[name] != type) {
            error("Name clash detected: '$name' is already taken by '${(type.classifier as KClass<*>).simpleName}'")
        } else resolvedTypes[name] = type
        return name
    }

    fun parameterValueSchema(routeParameter: KParameter): Schema<Any> {
        val type = routeParameter.type.classifier!!
        val schema = Schema<Any>()
        when (type) {
            Int::class -> {
                schema.addType("integer")
                schema.format = "int32"
            }
            Long::class -> {
                schema.addType("integer")
                schema.format = "int64"
            }
            Float::class -> {
                schema.addType("number")
                schema.format = "float"
            }
            Double::class -> {
                schema.addType("number")
                schema.format = "double"
            }
            Boolean::class -> schema.addType("boolean")
            String::class -> schema.addType("string")
            else -> error("Unsupported type: '$type'")
        }
        return schema
    }

    fun schemaRef(type: KType): Schema<Any> {
        val schema = Schema<Any>()
        when {
            type.isIterable() -> {
                val genericType = type.arguments.singleOrNull()?.type
                requireNotNull(genericType) { "failed to detect generic type of list" }
                schema.addType("array")
                schema.items = Schema<Any>().apply {
                    this.`$ref` = resolvedRef(genericType)
                }
            }
            type.isMap() -> schema.addType("object")
            else -> schema.`$ref` = resolvedRef(type)
        }
        return schema
    }

    fun addSchemas(openapi: OpenAPI) {
        openapi.components = Components()
        resolvedTypes.forEach { (name, kType) ->
            val schema = Schema<Any>()
            schema.addType("object")
            openapi.components.addSchemas(name, schema)
        }
    }
}