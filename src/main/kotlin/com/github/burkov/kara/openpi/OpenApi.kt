package com.github.burkov.kara.openapi

import com.github.burkov.kara.openapi.example.om
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.Paths
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import kara.*
import kara.internal.ParamRouteComponent
import kara.internal.ResourceDescriptor
import kara.internal.StringRouteComponent
import kara.internal.toRouteComponents
import kotlinx.reflection.boundReceiver
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KFunction
import kotlin.reflect.full.valueParameters


annotation class OpenApi()

@Location("/openapi")
@Controller("application/json")
object OpenApiController {
    @Get("schema")
    fun schema(): String {
        val schema = OpenAPI().apply {
            this.paths = Paths()
        }
        build(schema)
        return om.writeValueAsString(schema)
    }

    //    private val storedSchemas = mutableMapOf<String, Schema<*>>()

    @Suppress("UNCHECKED_CAST")
    private fun getResources(): Map<KAnnotatedElement, ResourceDescriptor> {
        val dispatcher = ActionContext.current().appContext.dispatcher
        val resourcesField = dispatcher.javaClass.getDeclaredField("resources")
        resourcesField.isAccessible = true
        return resourcesField.get(dispatcher) as Map<KAnnotatedElement, ResourceDescriptor>
    }

    private fun build(schema: OpenAPI) {
//        val om = Jackson.getObjectMapper().registerKotlinModule()
        getResources().forEach { (functionalRoute, descriptor) ->
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
//                                    this.schema = resolveSchemaRef(functionalRoute.returnType.javaType)
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

    //    private val context = ModelConverterContextImpl(ModelConverters.getInstance().converters)
//
//    private fun resolveSchemaRef(type: Type): Schema<*> {
//        var resolved = ModelConverters.getInstance().readAllAsResolvedSchema(type)
//        if (resolved == null) {
//            ModelResolver(Json.mapper()).resolve(AnnotatedType().type(type), context, null)
//            resolved = ModelConverters.getInstance().readAllAsResolvedSchema(type)
//        }
//        for ((name, schema) in resolved.referencedSchemas) {
//            storedSchemas[name] = schema
//        }
//        return Schema<Any>().`$ref`(resolved.schema.name)
//    }

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