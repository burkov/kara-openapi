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

class ResponseBuilder(private val response: ApiResponse) {
    lateinit var returnType: KType

    internal fun build() {
        if (returnType.classifier != Unit::class) {
            response.content = makeContent(returnType.javaType)
        }
    }
}

class RequestBuilder(private val operation: Operation) {
    var routeParams: List<KParameter> = emptyList()
    var queryParams: List<KParameter> = emptyList()
    var requestBody: KParameter? = null

    internal fun build() {
        requestBody?.let { operation.requestBody = processRequestBodyParameter(it) }
    }

    companion object {
        private fun processRequestBodyParameter(single: KParameter): RequestBody {
            return RequestBody().apply {
                this.required = !single.type.isMarkedNullable
                this.content = makeContent(single.type.javaType)
            }
        }
    }
}

class OperationBuilder(private val operation: Operation) {
    fun request(block: RequestBuilder.() -> Unit) {
        RequestBuilder(operation).apply(block).build()
    }

    fun response(block: ResponseBuilder.() -> Unit) {
        if (operation.responses == null) operation.responses = ApiResponses()
        val response = ApiResponse()
        ResponseBuilder(response).apply(block).build()
        operation.responses.addApiResponse("200", response)
    }
}

class PathBuilder(private val path: PathItem) {
    fun operation(method: PathItem.HttpMethod, block: OperationBuilder.() -> Unit) {
        val operation = Operation()
        OperationBuilder(operation).block()
        path.operation(method, operation)
    }
}


class OpenApiBuilder(val schemaMapper: SchemaMapper) {
    val openapi = OpenAPI().apply {
        this.paths = Paths()
    }

    fun build(): OpenAPI = openapi

    fun path(route: String, block: PathBuilder.() -> Unit) {
        val path = openapi.paths.getOrPut(route) { PathItem() }
        PathBuilder(path).block()
    }

    companion object {

    }
}

private fun makeContent(type: Type): Content {
    return Content().apply {
        this.addMediaType(applicationJsonMediaType, MediaType().apply {
//            this.schema = resolveSchemaRef(type)
        })
    }
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
