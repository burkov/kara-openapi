@file:OptIn(KspExperimental::class)

package com.github.burkov.kara.openapi.ksp.schema

import com.github.burkov.kara.openapi.ksp.KaraLocationController
import com.github.burkov.kara.openapi.ksp.kara.KaraAnnotations
import com.github.burkov.kara.openapi.ksp.kspLogger
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSValueParameter
import io.swagger.v3.core.util.Yaml31
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.PathItem
import kara.RequestBodyParameter
import kara.internal.ParamRouteComponent
import kara.internal.StringRouteComponent
import kara.internal.toRouteComponents
import java.io.File

object SchemaGenerator {
    private val ym = Yaml31.mapper()
    private val schemas = mutableMapOf<KSName, OpenAPI>()

    fun process(controllers: Iterable<KaraLocationController>, outputDir: String) {
        controllers.forEach { controller ->
            val builder = OpenApiBuilder("${controller.name.getShortName()} OpenApi specification")
            controller.routes.forEach { functionalRoute ->
                val route = normalizeRouteParams(functionalRoute, controller)
                val method = getFunctionalRouteMethod(functionalRoute)
                val routeParams = findRouteParameters(route, functionalRoute.parameters)
                val queryParams = findQueryParameters(route, functionalRoute.parameters)
                val requestBody = findRequestBodyParameter(functionalRoute.parameters)
                val returnType = functionalRoute.returnType!!.resolve()
                val hasNoResponseBody = returnType.declaration.isUnitType()

                builder.addOperation(route, method).let { operation ->
                    val name = if (hasNoResponseBody) "204" else "200"
                    builder.setResponse(operation, name, returnType)
                    builder.setRequestBody(operation, requestBody)
                    builder.setRouteParameters(operation, routeParams)
                    builder.setQueryParameters(operation, queryParams)
                    operation.operationId = functionalRoute.simpleName.getShortName()
                    operation.tags = listOf(controller.name.getShortName())
                }
            }
            schemas[controller.name] = builder.build()

            File(outputDir).mkdirs()

            schemas.forEach { (name, schema) ->
                val bytes = ym.writeValueAsBytes(schema)
                val subdirectories = name.getQualifier().replace(File.separator, "_").replace(".", File.separator)
                val fileName = "${subdirectories}${File.separator}${name.getShortName()}.yaml"
                val file = File(outputDir, fileName)
                file.parentFile.mkdirs()
                file.writeBytes(bytes)
            }
        }
    }

    private fun getFunctionalRouteMethod(functionalRoute: KSFunctionDeclaration): PathItem.HttpMethod {
        val routeAnnotation = KaraAnnotations.functionalRouteAnnotation(functionalRoute)!!
        return when (routeAnnotation.shortName.getShortName()) {
            "Get" -> PathItem.HttpMethod.GET
            "Post" -> PathItem.HttpMethod.POST
            "Delete" -> PathItem.HttpMethod.DELETE
            "Put" -> PathItem.HttpMethod.PUT
            "Options" -> PathItem.HttpMethod.OPTIONS
            else -> error("unknown route annotation $routeAnnotation")
        }
    }

    private fun findRouteParameters(route: String, params: List<KSValueParameter>): List<KSValueParameter> {
        val names = routeParamNames(route).toSet()
        val found = params.filter { names.contains(it.name!!.getShortName()) }
        val notFound = names.subtract(found.mapTo(mutableSetOf()) { it.name!!.getShortName() })
        if (notFound.isNotEmpty()) {
            kspLogger.error("Route $route has route parameters which have no actual parameter mapping: ${notFound.joinToString()}")
        }
        return found
    }

    private fun findQueryParameters(route: String, params: List<KSValueParameter>): List<KSValueParameter> {
        val names = routeParamNames(route).toSet()
        return params.filter { !it.isAnnotationPresent(RequestBodyParameter::class) && !names.contains(it.name!!.getShortName()) }
    }

    private fun findRequestBodyParameter(params: List<KSValueParameter>): KSValueParameter? {
        val requestBodyParameters = params.filter { it.isAnnotationPresent(RequestBodyParameter::class) }
        assert(requestBodyParameters.size <= 1) { "More than one request body parameter were found" }
        return requestBodyParameters.singleOrNull()
    }

    private fun routeParamNames(path: String): List<String> {
        return Regex("\\{([^\\}]*)\\}").findAll(path).map { it.groupValues[1] }.toList()
    }

    private fun normalizeRouteParams(func: KSFunctionDeclaration, controller: KaraLocationController): String {
        val routeAnnotation = KaraAnnotations.functionalRouteAnnotation(func)!!
        val path = routeAnnotation.arguments.single { it.name?.getShortName() == "route" }.value.toString()
        val components = path.toRouteComponents()
        return (controller.pathPrefix.toRouteComponents() + components).joinToString("/", prefix = "/") {
            when (it) {
                is ParamRouteComponent -> "{${it.name}}"
                is StringRouteComponent -> it.componentText
                else -> error("${it::class} route component is not supported")
            }
        }
    }

    fun KSDeclaration.isUnitType(): Boolean = this.qualifiedName?.asString() == "kotlin.Unit"

    //FIXME: don't use names
    fun KSDeclaration.isListLike(): Boolean =
        this.qualifiedName?.asString() in listOf("kotlin.collections.List", "kotlin.collections.Set")

    //FIXME don't use names
    fun KSDeclaration.isMapLike(): Boolean = this.qualifiedName?.asString() in listOf("kotlin.collections.Map")
}