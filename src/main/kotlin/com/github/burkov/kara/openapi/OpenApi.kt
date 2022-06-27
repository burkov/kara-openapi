package com.github.burkov.kara.openapi

import com.github.burkov.kara.openapi.misc.RoutesResolver
import io.swagger.v3.core.util.Yaml31
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.PathItem
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
    private val ym = Yaml31.mapper()


    @Get("schema.yaml")
    fun schemaYaml(): String = ym.writeValueAsString(schema)

    private fun build(): OpenAPI {
        val openapi = OpenApiBuilder.openapi()

        RoutesResolver.forEachFunctionalRoute { functionalRoute, descriptor ->
            val route = normalizeRouteParams(descriptor.route)
            val method = descriptor.httpMethod.toOpenAPIMethod()
            val routeParams = findRouteParameters(functionalRoute.valueParameters, descriptor.route)
            val queryParams = findQueryParameters(functionalRoute.valueParameters, descriptor.route)
            val requestBody = findRequestBodyParameter(functionalRoute.valueParameters)
            val returnType = functionalRoute.returnType
            val hasNoResponseBody = returnType.isUnitKType()

            OpenApiBuilder.addOperation(openapi, route, method).let { operation ->
                val name = if (hasNoResponseBody) "204" else "200"
                OpenApiBuilder.setResponse(operation, name, returnType)
                OpenApiBuilder.setRequestBody(operation, requestBody)
                OpenApiBuilder.setRouteParameters(operation, routeParams)
                OpenApiBuilder.setQueryParameters(operation, queryParams)
            }
        }
        return openapi
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

    private fun isRequestBodyParameter(p: KParameter): Boolean =
        p.annotations.filterIsInstance<RequestBodyParameter>().isNotEmpty()

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