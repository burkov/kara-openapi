package com.github.burkov.kara.openapi

import com.github.burkov.kara.openapi.example.om
import io.swagger.v3.core.converter.ModelConverterContextImpl
import io.swagger.v3.core.converter.ModelConverters
import io.swagger.v3.oas.models.*
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import kara.*
import kara.internal.ParamRouteComponent
import kara.internal.ResourceDescriptor
import kara.internal.StringRouteComponent
import kara.internal.toRouteComponents
import kotlinx.reflection.boundReceiver
import java.lang.reflect.Type
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.javaType


annotation class OpenApi()

const val applicationJsonMediaType = "application/json"

@Get("/ui")
object UI : Request({
    object : BaseActionResult("text/html", 200, content = {
        """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="utf-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1" />
              <meta
                name="description"
                content="SwaggerIU"
              />
              <title>SwaggerUI</title>
              <link rel="stylesheet" href="https://unpkg.com/swagger-ui-dist@4.5.0/swagger-ui.css" />
            </head>
            <body>
            <div id="swagger-ui"></div>
            <script src="https://unpkg.com/swagger-ui-dist@4.5.0/swagger-ui-bundle.js" crossorigin></script>
            <script>
              window.onload = () => {
                window.ui = SwaggerUIBundle({
                  url: 'http://localhost:8080/openapi/schema',
                  dom_id: '#swagger-ui',
                });
              };
            </script>
            </body>
            </html>
        """.trimIndent()
    }) {}
})


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

    private val storedSchemas = mutableMapOf<String, Schema<*>>()

    @Suppress("UNCHECKED_CAST")
    private fun getResources(): Map<KAnnotatedElement, ResourceDescriptor> {
        val dispatcher = ActionContext.current().appContext.dispatcher
        val resourcesField = dispatcher.javaClass.getDeclaredField("resources")
        resourcesField.isAccessible = true
        return resourcesField.get(dispatcher) as Map<KAnnotatedElement, ResourceDescriptor>
    }

    private fun build(schema: OpenAPI) {
        getResources().forEach { (functionalRoute, descriptor) ->
            if (functionalRoute is KFunction<*>) {
                val isMarked = funcHasOpenApiMarker(functionalRoute) || receiverHasOpenApiMarker(functionalRoute)
                if (isMarked) {
                    val path = schema.paths.getOrPut(normalizePathParams(descriptor.route)) { PathItem() }
                    val operation = Operation().apply { this.responses = ApiResponses() }
                    val returnType = functionalRoute.returnType
                    operation.responses.addApiResponse("200", ApiResponse().apply {
                        if (returnType.classifier != Unit::class) {
                            this.content = makeContent(returnType.javaType)
                        }
                    })
                    val params = functionalRoute.valueParameters
                    when {
                        params.size > 1 -> TODO("implement me")
                        params.size == 1 && params.single().isRequestBodyParameter() -> processRequestBodyParameter(operation, params.single()) // FIXME

                        else -> Unit
                    }
                    path.operation(toOpenAPIMethod(descriptor.httpMethod), operation)
//                    println("${descriptor.route} ${functionalRoute.returnType}")
                }
            }
        }

        schema.components = Components().apply {
            storedSchemas.forEach { (name, schema) ->
                this.addSchemas(name, schema)
            }
        }
    }

    private val context = ModelConverterContextImpl(ModelConverters.getInstance().converters)

    private val modelConverters = ModelConverters.getInstance()

    private fun resolveSchemaRef(type: Type): Schema<*> {
        var resolved = modelConverters.readAllAsResolvedSchema(type)
//        if (resolved == null) {
//            ModelResolver(om).resolve(AnnotatedType().type(type), context, null)
//            resolved = OpenApiController.modelConverters().readAllAsResolvedSchema(type)
//        }
        for ((name, schema) in resolved.referencedSchemas) {
            storedSchemas[name] = schema
        }
        return Schema<Any>().`$ref`(resolved.schema.name)
    }

    private fun processRequestBodyParameter(operation: Operation, single: KParameter) {
        operation.requestBody = RequestBody().apply {
            this.required = !single.type.isMarkedNullable
            this.content = makeContent(single.type.javaType)
        }
    }

    private fun makeContent(type: Type): Content {
        return Content().apply {
            this.addMediaType(applicationJsonMediaType, MediaType().apply {
                this.schema = resolveSchemaRef(type)
            })
        }
    }

    private fun KParameter.isRequestBodyParameter(): Boolean {
        return this.annotations.filterIsInstance<RequestBodyParameter>().isNotEmpty()
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