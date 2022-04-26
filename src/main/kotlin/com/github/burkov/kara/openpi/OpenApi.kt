package com.github.burkov.kara.openpi

import io.swagger.v3.core.converter.*
import io.swagger.v3.core.jackson.ModelResolver
import io.swagger.v3.core.util.Json
import io.swagger.v3.oas.models.*
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import kara.*
import kara.internal.*
import kotlinx.reflection.boundReceiver
import java.lang.reflect.Type
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaType


annotation class OpenApi()

//@Location("/openapi")
//@Controller("application/json")
object OpenApiController {
    @Get("schema")
    fun schema(): String {
        val schema = OpenAPI().apply {
            this.paths = Paths()
        }
        build(schema)
        return JsonUtils.writeValueAsString(schema)
    }

    private val storedSchemas = mutableMapOf<String, Schema<*>>()

    fun build(schema: OpenAPI) {

        val om = Json.mapper()
        val dispatcher = ActionContext.current().appContext.dispatcher
        val resourcesField = dispatcher.javaClass.getDeclaredField("resources")
        resourcesField.isAccessible = true
        val resources = resourcesField.get(dispatcher) as Map<KAnnotatedElement, ResourceDescriptor>
        resources.forEach { (functionalRoute, descriptor) ->
            if (functionalRoute is KFunction<*>) {
                val isMarked = funcHasOpenApiMarker(functionalRoute) || receiverHasOpenApiMarker(functionalRoute)
                if (isMarked) {
                    val path = schema.paths.getOrPut(normalizePathParams(descriptor.route)) { PathItem() }
                    val operation = Operation().apply { this.responses = ApiResponses() }
                    val returnType = functionalRoute.returnType
                    operation.responses.addApiResponse("200", ApiResponse().apply {
                        if (returnType.classifier != Unit::class) {
                            this.content = Content().apply {
                                this.addMediaType("application/json", MediaType().apply {
                                    this.schema = resolveSchemaRef(functionalRoute.returnType.javaType)
                                })
                            }
                        }
                    })

                    path.operation(toOpenAPIMethod(descriptor.httpMethod), operation)
                    println("${descriptor.route} ${functionalRoute.returnType}")
//                    println(functionalRoute.valueParameters)
                }
            }
        }
    }

    private val context = ModelConverterContextImpl(ModelConverters.getInstance().converters)

    private fun resolveSchemaRef(type: Type): Schema<*> {
        var resolved = ModelConverters.getInstance().readAllAsResolvedSchema(type)
        if (resolved == null) {
            ModelResolver(Json.mapper()).resolve(AnnotatedType().type(type), context, null)
            resolved = ModelConverters.getInstance().readAllAsResolvedSchema(type)
        }
        for ((name, schema) in resolved.referencedSchemas) {
            storedSchemas[name] = schema
        }
        return Schema<Any>().`$ref`(resolved.schema.name)
    }

    private fun toOpenAPIMethod(karaMethod: HttpMethod): PathItem.HttpMethod = when (karaMethod) {
        HttpMethod.GET -> PathItem.HttpMethod.GET
        HttpMethod.POST -> PathItem.HttpMethod.POST
        HttpMethod.DELETE -> PathItem.HttpMethod.DELETE
        HttpMethod.PUT -> PathItem.HttpMethod.PUT
        HttpMethod.OPTIONS -> PathItem.HttpMethod.OPTIONS
        else -> TODO("not implemented")
    }

    private fun funcHasOpenApiMarker(func: KFunction<*>): Boolean {
        return func.annotations.filterIsInstance<OpenApi>().isNotEmpty()
    }

    private fun receiverHasOpenApiMarker(func: KFunction<*>): Boolean {
        val receiverClass = func.boundReceiver()?.javaClass
        return receiverClass?.getAnnotation(OpenApi::class.java) != null
    }

    private fun normalizePathParams(path: String): String {
        val components = path.toRouteComponents()
        return components.joinToString("/") {
            when (it) {
                is ParamRouteComponent -> "{${it.name}}"
                is StringRouteComponent -> it.componentText
                else -> error("${it::class} route component is not supported")
            }
        }
    }
}
