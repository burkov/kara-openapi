package com.github.burkov.kara.openapi

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.Paths
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import java.lang.reflect.Type
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.jvm.javaType

class OpenApiBuilder {
    private val openapi = OpenAPI().also {
        it.paths = Paths()
    }
    private val schemaMapper = SchemaMapper()

    fun build() = openapi

    fun addOperation(route: String, method: PathItem.HttpMethod): Operation {
        val operation = Operation().also {
            it.responses = ApiResponses()
        }
        val pathItem = openapi.paths.getOrPut(route) { PathItem() }
        pathItem.operation(method, operation)
        return operation
    }

    fun setResponse(operation: Operation, name: String, returnType: KType): ApiResponse {
        val apiResponse = operation.responses.getOrPut(name) { ApiResponse() }
        if (!returnType.isUnitKType()) {
            apiResponse.content = makeContent(returnType.javaType)
        }
        return apiResponse
    }

    fun setRequestBody(operation: Operation, requestBody: KParameter?) {
        if (requestBody == null) return
        operation.requestBody = RequestBody().apply {
            this.required = !requestBody.type.isMarkedNullable
            this.content = makeContent(requestBody.type.javaType)
        }
    }

    fun setRouteParameters(operation: Operation, routeParams: List<KParameter>) {
//        println("Setting route params")
    }

    fun setQueryParameters(operation: Operation, queryParams: List<KParameter>) {
//        println("Setting query params")
    }

    private fun makeContent(type: Type): Content {
        val content = Content()
        val mediaType = MediaType()
        mediaType.schema = schemaMapper.schemaRef(type)
        content.addMediaType(applicationJsonMediaType, mediaType)
        return content
    }
}

fun KType.isUnitKType(): Boolean {
    return this.classifier != Unit::class
}


//private val modelConverters = ModelConverters.getInstance()
//
//private fun resolveSchemaRef(type: Type): Schema<*> {
//    val resolvedSchema = modelConverters.readAllAsResolvedSchema(type)
//    resolvedSchema.schema.name = type.typeName.substringAfter("openapi.")
//    if (resolvedSchema == null) {
//        print("WTF")
//        error("WTF")
////            ModelResolver(om).resolve(AnnotatedType().type(type), context, null)
////            resolved = OpenApiController.modelConverters().readAllAsResolvedSchema(type)
//    }
//    for (schema in resolvedSchema.referencedSchemas.values) {
//        println("RESOLVED: ${schema.name}")
////            storedSchemas[schema.name] = schema
//    }
//    return Schema<Any>().`$ref`(resolvedSchema.schema.name)
//}
