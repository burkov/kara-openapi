package com.github.burkov.kara.openapi.ksp.schema

import com.github.burkov.kara.openapi.ksp.kspLogger
import com.github.burkov.kara.openapi.ksp.schema.SchemaGenerator.isListLike
import com.github.burkov.kara.openapi.ksp.schema.SchemaGenerator.isMapLike
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema

class SchemaMapper {
    private val resolvedTypes = mutableMapOf<String, KSType>()

    private fun resolvedTypeName(type: KSType): String {
        return type.declaration.simpleName.asString()
    }

    private fun resolvedRef(type: KSType): String {
        val name = resolvedTypeName(type)
        val previouslyResolved = resolvedTypes[name]
        return when {
            previouslyResolved == null -> name.also { resolvedTypes[name] = type }
            previouslyResolved != type -> error("Name clash detected: '$name' is already taken by '${previouslyResolved}'")
            else -> name
        }
    }

    fun primitiveTypeSchema(type: KSType): Schema<Any>? {
        val typeName = type.declaration.qualifiedName!!.asString()
        val schema = Schema<Any>()
        when (typeName) {
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
            else -> return null
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
        resolvedTypes.forEach { (name, type) ->
            val schema = resolveSchema(type)
            openapi.components.addSchemas(name, schema)
        }
    }

    private fun resolveSchema(type: KSType): Schema<Any> {
        require(type.declaration is KSClassDeclaration) { "Trying to resolve schema for a non-class type: $type" }
        val decl = type.declaration as KSClassDeclaration
        val schema = Schema<Any>()
        schema.addType("object")
        decl.getAllProperties().forEach {
            val propertyName = it.simpleName.getShortName()
            val propertyType = it.type.resolve()
            val propertySchema = primitiveTypeSchema(propertyType)
            require(propertySchema !== null) { "Implement me: nested data classes are not supported yet" }
            schema.addProperty(propertyName, propertySchema)
        }
        schema.required = decl.getAllProperties().mapNotNull { property ->
            property.takeIf { !property.type.resolve().isMarkedNullable }?.simpleName?.getShortName()
        }.toList()
        return schema
    }
}