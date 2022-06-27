package com.github.burkov.kara.openapi

import io.swagger.v3.oas.models.media.Schema
import java.lang.reflect.Type
import kotlin.reflect.KType

class SchemaMapper {
    private val storedSchemas = mutableMapOf<String, Schema<*>>()
//    private val resolved = mutableMapOf<KType, Schema>()


    fun schemaRef(type: Type): Schema<Any> {
        val schema = Schema<Any>()
        val name = type.typeName
        schema.`$ref` = "#components/schemas/$name.yaml"
        return schema
    }
}