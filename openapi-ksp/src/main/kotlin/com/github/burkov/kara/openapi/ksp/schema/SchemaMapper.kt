package com.github.burkov.kara.openapi.ksp.schema

import com.github.burkov.kara.openapi.ksp.kspBuiltIns
import com.github.burkov.kara.openapi.ksp.kspLogger
import com.github.burkov.kara.openapi.ksp.schema.SchemaGenerator.isListLike
import com.github.burkov.kara.openapi.ksp.schema.SchemaGenerator.isMapLike
import com.github.burkov.kara.openapi.ksp.schema.SchemaGenerator.isUnitType
import com.google.devtools.ksp.processing.KSBuiltIns
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema
import kotlin.reflect.KClass
import kotlin.reflect.KType

class SchemaMapper {
    private val resolvedTypes = mutableMapOf<String, KSType>()

    private fun resolvedRef(type: KSType): String {
        val name = type.declaration.simpleName.asString()
        if (resolvedTypes[name] != null && resolvedTypes[name] != type) {
            error("Name clash detected: '$name' is already taken by '${resolvedTypes[name]!!.declaration.simpleName.getShortName()}'")
        } else resolvedTypes[name] = type
        return name
    }

    fun parameterValueSchema(routeParameter: KSValueParameter): Schema<Any> {
        val type = routeParameter.type.resolve().declaration.qualifiedName!!.asString()
        val schema = Schema<Any>()
        when (type) {
            "kotlin.Int" -> {
                schema.addType("integer")
                schema.format = "int32"
            }
            "kotlin.Long" -> {
                schema.addType("integer")
                schema.format = "int64"
            }
            "kotlin.Float" -> {
                schema.addType("number")
                schema.format = "float"
            }
            "kotlin.Double" -> {
                schema.addType("number")
                schema.format = "double"
            }
            "kotlin.Boolean" -> schema.addType("boolean")
            "kotlin.String" -> schema.addType("string")
            else -> error("Unsupported type: '$type'")
        }
        return schema
    }

    fun schemaRef(type: KSType): Schema<Any> {
        val schema = Schema<Any>()
        when {
            type.declaration.isListLike() -> {
                val genericType = type.arguments.singleOrNull()?.type?.resolve()
                requireNotNull(genericType) { "failed to detect generic type of list" }
                schema.addType("array")
                schema.items = Schema<Any>().apply {
                    this.`$ref` = resolvedRef(genericType)
                }
            }
            type.declaration.isMapLike() -> schema.addType("object")
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