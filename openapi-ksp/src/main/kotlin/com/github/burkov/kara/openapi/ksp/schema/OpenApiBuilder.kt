package com.github.burkov.kara.openapi.ksp.schema

import com.github.burkov.kara.openapi.ksp.schema.SchemaGenerator.isUnitType
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter
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
import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf

const val applicationJsonMediaType = "application/json"

class OpenApiBuilder(private val title: String) {
    private val openapi = OpenAPI().also {
        it.paths = Paths()
        it.info = Info()
        it.info.title = title
        it.info.version = "0.0.1"
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

    fun setResponse(operation: Operation, name: String, returnType: KSType): ApiResponse {
        val apiResponse = operation.responses.getOrPut(name) { ApiResponse() }
        apiResponse.content = makeContent(returnType)
        apiResponse.description = "OK"
        return apiResponse
    }

    fun setRequestBody(operation: Operation, requestBody: KSValueParameter?) {
        if (requestBody == null) return
        val type = requestBody.type.resolve()

        operation.requestBody = RequestBody().apply {
            this.required = !type.isMarkedNullable
            this.content = makeContent(type)
        }
    }

    fun setRouteParameters(operation: Operation, routeParams: List<KSValueParameter>) {
        routeParams.forEach { routeParameter ->
            requireNotNull(routeParameter.name) { "Nameless route parameter $routeParameter" }
            val parameter = Parameter()
            parameter.`in` = "path"
            parameter.required = true
            parameter.name = routeParameter.name!!.getShortName()
            parameter.schema = schemaMapper.parameterValueSchema(routeParameter)
            operation.addParametersItem(parameter)
        }
    }

    fun setQueryParameters(operation: Operation, queryParams: List<KSValueParameter>) {
//        println("Setting query params")
    }

    private fun makeContent(type: KSType): Content? {
        if (type.declaration.isUnitType()) return null
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
