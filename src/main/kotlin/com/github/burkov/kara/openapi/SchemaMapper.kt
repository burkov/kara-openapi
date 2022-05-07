package com.github.burkov.kara.openapi
import io.swagger.v3.oas.models.media.Schema
import kotlin.reflect.KType

class SchemaMapper {
    private val storedSchemas = mutableMapOf<String, Schema<*>>()
//    private val resolved = mutableMapOf<KType, Schema>()

    fun resolveRef(type: KType): String {
        return ""
    }
}