package com.github.burkov.kara.openapi.example

import kara.ApplicationConfig
import kara.server.JettyRunner
import kotlinx.reflection.Serialization


fun main() {
    Serialization.register(AnySerializer)
    val applicationConfig = ApplicationConfig(InMemoryConfigProvider, ApplicationConfig::class.java.classLoader!!)
    val runner = JettyRunner(applicationConfig)
    runner.start()
}