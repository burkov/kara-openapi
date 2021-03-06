@file:OptIn(KspExperimental::class)

package com.github.burkov.kara.openapi.ksp

import com.github.burkov.kara.openapi.annotations.OpenApi
import com.github.burkov.kara.openapi.ksp.kara.KaraAnnotations
import com.github.burkov.kara.openapi.ksp.schema.SchemaGenerator
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.KSBuiltIns
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.*
import kara.Location


data class KaraLocationController(
    val name: KSName,
    val pathPrefix: String,
    val routes: MutableSet<KSFunctionDeclaration> = mutableSetOf()
)

lateinit var kspLogger: KSPLogger
lateinit var kspBuiltIns: KSBuiltIns

class OpenApiProcessor(private val outputDir: String) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        kspBuiltIns = resolver.builtIns
        val controllers = mutableMapOf<KSName, KaraLocationController>()
        val symbols = resolver.getSymbolsWithAnnotation(OpenApi::class.qualifiedName!!)

        fun validateAndGetLocationController(symbol: KSNode?): KaraLocationController {
            validateController(symbol)
            val name = (symbol as KSClassDeclaration).qualifiedName!!
            return controllers.getOrPut(name) {
                KaraLocationController(
                    name = name,
                    pathPrefix = controllerPathPrefix(symbol)
                )
            }
        }

        fun validateRoutesAndAdd(controller: KaraLocationController, list: List<KSFunctionDeclaration>) {
            val functionalRoutes = list.filter(::isFunctionalRoute)
            if (functionalRoutes.isEmpty())
                kspLogger.error("Passed controller (${controller.name}) functions list (${list.size} items: ${list.joinToString { it.simpleName.asString() }}) has no functional routes")
            controller.routes.addAll(functionalRoutes)
        }

        symbols.forEach { symbol ->
            when (symbol) {
                is KSClassDeclaration -> {
                    val controller = validateAndGetLocationController(symbol)
                    validateRoutesAndAdd(controller, symbol.getAllFunctions().toList())
                }
                is KSFunctionDeclaration -> {
                    val controller = validateAndGetLocationController(symbol.parent)
                    validateRoutesAndAdd(controller, listOf(symbol))
                }
            }
        }
        SchemaGenerator.process(controllers.values, outputDir)
        return emptyList()
    }


    companion object {
        private const val CONTROLLER_SUFFIX = "Controller"
        private fun controllerPathPrefix(symbol: KSClassDeclaration): String {
            return symbol.getAnnotationsByType(Location::class).single().path
        }

        fun isFunctionalRoute(symbol: KSFunctionDeclaration): Boolean {
            return KaraAnnotations.functionalRouteAnnotation(symbol) != null
        }

        private fun validateController(symbol: KSNode?) {
            when {
                symbol !is KSClassDeclaration ->
                    kspLogger.error("Symbol is not a KSClassDeclaration, $symbol")
                symbol.qualifiedName == null ->
                    kspLogger.error("Controller has no qualified name ${symbol.packageName}.${symbol.simpleName}")
                !symbol.simpleName.getShortName().endsWith(CONTROLLER_SUFFIX) ->
                    kspLogger.error("Kara route marked with @OpenApi annotation should end with -$CONTROLLER_SUFFIX suffix")
                symbol.getAnnotationsByType(Location::class).firstOrNull() == null ->
                    kspLogger.error("Only @Location controller should be annotated with @OpenApi (${symbol.qualifiedName})")
            }
        }
    }
}