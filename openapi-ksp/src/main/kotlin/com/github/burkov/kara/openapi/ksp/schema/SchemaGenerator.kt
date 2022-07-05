package com.github.burkov.kara.openapi.ksp.schema

import com.github.burkov.kara.openapi.ksp.KaraLocationController
import com.github.burkov.kara.openapi.ksp.kara.KaraAnnotations
import com.github.burkov.kara.openapi.ksp.kspLogger
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import io.swagger.v3.oas.models.PathItem
import kara.Get
import kara.HttpMethod
import kara.internal.ParamRouteComponent
import kara.internal.StringRouteComponent
import kara.internal.toRouteComponents

object SchemaGenerator {

    fun process(controllers: Iterable<KaraLocationController>) {
        val builder = OpenApiBuilder()
        controllers.forEach { controller ->
            controller.routes.forEach { functionalRoute ->
                val route = normalizeRouteParams(functionalRoute, controller)
                val method = getFunctionalRouteMethod(functionalRoute)

                kspLogger.warn("$route is $method")
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
}