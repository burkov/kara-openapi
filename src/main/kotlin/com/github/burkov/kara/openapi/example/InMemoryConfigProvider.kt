package com.github.burkov.kara.openapi.example

import tanvd.konfy.provider.ConfigProvider
import java.lang.reflect.Type

object InMemoryConfigProvider : ConfigProvider() {
    override fun <N : Any> fetch(key: String, type: Type): N? {
        return when (key) {
            "kara.routePackages" -> arrayOf("com.github.burkov.kara.openapi") as N
            "kara.environment" -> "development" as N
            "kara.port" -> "8080" as N
            else -> null
        }
    }
}