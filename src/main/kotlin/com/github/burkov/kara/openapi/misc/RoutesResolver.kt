package com.github.burkov.kara.openapi.misc

import com.github.burkov.kara.openapi.annotations.OpenApi
import kara.ActionContext
import kara.internal.ResourceDescriptor
import kotlinx.reflection.boundReceiver
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KFunction

data class LocationController(
    val name: String,
    val self: Any,
    val routes: MutableList<Pair<KFunction<*>, ResourceDescriptor>>
)

object RoutesResolver {
    fun forEachController(block: (LocationController) -> Unit) {
        val dispatcher = ActionContext.current().appContext.dispatcher
        val resourcesField = dispatcher.javaClass.getDeclaredField("resources")
        resourcesField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val resources = resourcesField.get(dispatcher) as Map<KAnnotatedElement, ResourceDescriptor>
        val controllers = mutableMapOf<String, LocationController>()
        resources.forEach { (functionalRoute, descriptor) ->
            if (functionalRoute is KFunction<*>) {
                val boundReceiver = functionalRoute.boundReceiver()
                val receiverName = boundReceiver?.javaClass?.name
                val isMarked = boundReceiver?.javaClass?.getAnnotation(OpenApi::class.java) != null
                if (receiverName != null && isMarked) {
                    require(receiverName.endsWith("Controller")) { "Kara route marked with @OpenApi annotation should end with -Controller suffix" }
                    val controller = controllers.getOrPut(receiverName) {
                        LocationController(
                            name = receiverName,
                            self = boundReceiver,
                            routes = mutableListOf()
                        )
                    }
                    controller.routes.add(functionalRoute to descriptor)
                }
            }
        }
        controllers.values.forEach(block)
    }
}
