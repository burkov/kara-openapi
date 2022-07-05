package com.github.burkov.kara.openapi

import io.swagger.v3.core.util.Yaml31
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.PathItem
import kara.*
import kara.internal.ParamRouteComponent
import kara.internal.toRouteComponents
import kotlin.reflect.KParameter

@Location("/openapi")
@Controller("application/json")
object OpenApiController {
    private val schemas by lazy { build() }
    private val ym = Yaml31.mapper()

    @Get("/:name")
    fun apiSchema(name: String): String {
        if (!schemas.containsKey(name)) throw ResultWithCodeException(404, "Not found")
        return ym.writeValueAsString(schemas[name])
    }

    private fun build(): Map<String, OpenAPI> {
        val result = mutableMapOf<String, OpenAPI>()
//        RoutesResolver.forEachController { controller ->
//            controller.routes.forEach { (functionalRoute, descriptor) ->
//
//                val routeParams = findRouteParameters(functionalRoute.valueParameters, descriptor.route)
//                val queryParams = findQueryParameters(functionalRoute.valueParameters, descriptor.route)
//                val requestBody = findRequestBodyParameter(functionalRoute.valueParameters)
//                val returnType = functionalRoute.returnType
//                val hasNoResponseBody = returnType.isUnitKType()
//
//                builder.addOperation(route, method).let { operation ->
//                    val name = if (hasNoResponseBody) "204" else "200"
//                    builder.setResponse(operation, name, returnType)
//                    builder.setRequestBody(operation, requestBody)
//                    builder.setRouteParameters(operation, routeParams)
//                    builder.setQueryParameters(operation, queryParams)
//                    operation.operationId = functionalRoute.name
//                    operation.tags = listOf(controller.self::class.simpleName)
//                }
//            }
//            result[controller.name] = builder.build()
//        }
        return result
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


}