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
) {
    init {
        require(name.endsWith(SUFFIX)) { "Kara route marked with @OpenApi annotation should end with -Controller suffix" }
    }

    companion object {
        private const val SUFFIX = "Controller"
    }
}

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
                val isMarked = isReceiverMarked(boundReceiver) || isFunctionMarked(functionalRoute)
                if (receiverName != null && isMarked) {
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

    private fun isFunctionMarked(functionalRoute: KAnnotatedElement) =
        functionalRoute.annotations.filterIsInstance<OpenApi>()
            .isNotEmpty()

    private fun isReceiverMarked(boundReceiver: Any?) =
        boundReceiver?.javaClass?.getAnnotation(OpenApi::class.java) != null
}
