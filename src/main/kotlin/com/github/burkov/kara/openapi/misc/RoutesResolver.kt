package com.github.burkov.kara.openapi.misc

import com.github.burkov.kara.openapi.annotations.OpenApi
import kara.ActionContext
import kara.internal.ResourceDescriptor
import kotlinx.reflection.boundReceiver
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KFunction


object RoutesResolver {
    fun forEachFunctionalRoute(block: (function: KFunction<*>, descriptor: ResourceDescriptor) -> Unit) {
        val dispatcher = ActionContext.current().appContext.dispatcher
        val resourcesField = dispatcher.javaClass.getDeclaredField("resources")
        resourcesField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val resources = resourcesField.get(dispatcher) as Map<KAnnotatedElement, ResourceDescriptor>
        resources.forEach { (functionalRoute, descriptor) ->
            if (functionalRoute is KFunction<*>) {
                val isMarked = funcHasOpenApiMarker(functionalRoute) || receiverHasOpenApiMarker(functionalRoute)
                if (isMarked) {
                    block(functionalRoute, descriptor)
                }
            }
        }
    }

    private fun funcHasOpenApiMarker(func: KFunction<*>): Boolean {
        return func.annotations.filterIsInstance<OpenApi>().isNotEmpty()
    }

    private fun receiverHasOpenApiMarker(func: KFunction<*>): Boolean {
        val receiverClass = func.boundReceiver()?.javaClass
        return receiverClass?.getAnnotation(OpenApi::class.java) != null
    }
}
