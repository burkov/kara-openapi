package com.github.burkov.kara.openapi

import com.github.burkov.kara.openapi.example.om
import com.github.burkov.kara.openapi.example.ym
import com.github.burkov.kara.openapi.misc.*
import io.swagger.v3.oas.models.*
import kara.*
import kara.internal.ParamRouteComponent
import kara.internal.StringRouteComponent
import kara.internal.toRouteComponents
import kotlin.reflect.KParameter
import kotlin.reflect.full.valueParameters

const val applicationJsonMediaType = "application/json"

@Location("/openapi")
@Controller("application/json")
object OpenApiController {
    private val schema by lazy { build() }

    @Get("schema.yaml")
    fun schemaYaml(): String = ym.writeValueAsString(schema)

    @Get("schema.json")
    fun schemaJson(): String = om.writeValueAsString(schema)

    private fun build(): OpenAPI {
        return OpenApiBuilder(SchemaMapper()).apply {
            RoutesResolver.forEachFunctionalRoute { functionalRoute, descriptor ->
                path(normalizeRouteParams(descriptor.route)) {
                    operation(descriptor.httpMethod.toOpenAPIMethod()) {
                        request {
                            routeParams = findRouteParameters(functionalRoute.valueParameters, descriptor.route)
                            queryParams = findQueryParameters(functionalRoute.valueParameters, descriptor.route)
                            requestBody = findRequestBodyParameter(functionalRoute.valueParameters)
                        }
                        response {
                            returnType = functionalRoute.returnType
                        }
                    }
                }
            }
        }.build()
    }

    private fun findRouteParameters(params: List<KParameter>, route: String): List<KParameter> {
        val names = routeParamNames(route).toSet()
        return params.filter { names.contains(it.name) }
    }

    private fun findQueryParameters(params: List<KParameter>, route: String): List<KParameter> {
        val names = routeParamNames(route).toSet()
        return params.filter { !isRequestBodyParameter(it) && !names.contains(it.name) }
    }

    private fun findRequestBodyParameter(params: List<KParameter>): KParameter? {
        val requestBodyParameters = params.filter(::isRequestBodyParameter)
        assert(requestBodyParameters.size <= 1) { "More than one request body parameter were found" }
        return requestBodyParameters.singleOrNull()
    }

    private fun isRequestBodyParameter(p: KParameter): Boolean = p.annotations.filterIsInstance<RequestBodyParameter>().isNotEmpty()

    private fun HttpMethod.toOpenAPIMethod(): PathItem.HttpMethod = when (this) {
        HttpMethod.GET -> PathItem.HttpMethod.GET
        HttpMethod.POST -> PathItem.HttpMethod.POST
        HttpMethod.DELETE -> PathItem.HttpMethod.DELETE
        HttpMethod.PUT -> PathItem.HttpMethod.PUT
        HttpMethod.OPTIONS -> PathItem.HttpMethod.OPTIONS
        else -> TODO("not implemented")
    }

    private fun routeParamNames(path: String): List<String> {
        return path.toRouteComponents().filterIsInstance<ParamRouteComponent>().map { it.name }
    }

    private fun normalizeRouteParams(path: String): String {
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