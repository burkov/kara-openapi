package com.github.burkov.kara.openapi.ksp.schema

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.Paths
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf

const val applicationJsonMediaType = "application/json"

class OpenApiBuilder {
    private val openapi = OpenAPI().also {
        it.paths = Paths()
        it.info = Info()
        it.info.title = "fixme"
        it.info.version = "v1"
    }
    private val schemaMapper = SchemaMapper()

    fun build(): OpenAPI {
        schemaMapper.addSchemas(openapi)
        return openapi
    }

    fun addOperation(route: String, method: PathItem.HttpMethod): Operation {
        val operation = Operation().also {
            it.responses = ApiResponses()
        }
        val normalizedRoute = if (route.startsWith("/")) route else "/$route"
        val pathItem = openapi.paths.getOrPut(normalizedRoute) { PathItem() }
        pathItem.operation(method, operation)
        return operation
    }

    fun setResponse(operation: Operation, name: String, returnType: KType): ApiResponse {
        val apiResponse = operation.responses.getOrPut(name) { ApiResponse() }
        apiResponse.content = makeContent(returnType)
        apiResponse.description = "fixme"
        return apiResponse
    }

    fun setRequestBody(operation: Operation, requestBody: KParameter?) {
        if (requestBody == null) return
        operation.requestBody = RequestBody().apply {
            this.required = !requestBody.type.isMarkedNullable
            this.content = makeContent(requestBody.type)
        }
    }

    fun setRouteParameters(operation: Operation, routeParams: List<KParameter>) {
        routeParams.forEach { routeParameter ->
            requireNotNull(routeParameter.name) { "Nameless route parameter $routeParameter" }
            val parameter = Parameter()
            parameter.`in` = "path"
            parameter.required = true
            parameter.name = routeParameter.name
            parameter.schema = schemaMapper.parameterValueSchema(routeParameter)

            operation.addParametersItem(parameter)
        }
//        println("Setting route params")
    }

    fun setQueryParameters(operation: Operation, queryParams: List<KParameter>) {
//        println("Setting query params")
    }

    private fun makeContent(type: KType): Content? {
        if (type.isUnitKType()) return null
        val content = Content()
        val mediaType = MediaType()
        content.addMediaType(applicationJsonMediaType, mediaType)
        mediaType.schema = schemaMapper.schemaRef(type)
        return content
    }
}

fun KType.isUnitKType(): Boolean {
    return this.classifier == Unit::class
}

fun KType.isMap(): Boolean {
    return ((this.classifier as? KClass<*>)?.isSubclassOf(Map::class)) ?: false
}

fun KType.isIterable(): Boolean {
    return ((this.classifier as? KClass<*>)?.isSubclassOf(Iterable::class)) ?: false
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
