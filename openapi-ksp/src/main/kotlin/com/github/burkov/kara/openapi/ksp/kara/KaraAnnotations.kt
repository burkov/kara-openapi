package com.github.burkov.kara.openapi.ksp.kara

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import kara.Delete
import kara.Get
import kara.Post
import kara.Put

object KaraAnnotations {
    fun functionalRouteAnnotation(symbol: KSFunctionDeclaration): KSAnnotation? {
        val markers = listOf(
            Get::class, Post::class, Put::class, Delete::class
        )
        val found = symbol.annotations.filter { a ->
            when { // when is used for lazy resolving in else branch
                markers.none { it.simpleName == a.shortName.getShortName() } -> false
                else -> markers.any {
                    it.qualifiedName == a.annotationType.resolve().declaration.qualifiedName?.asString()
                }
            }
        }.toList()
        if (found.isEmpty()) return null
        require(found.size == 1) { "${symbol.simpleName.getShortName()} has more than 1 kara route annotation" }

        return found.singleOrNull()
    }
}