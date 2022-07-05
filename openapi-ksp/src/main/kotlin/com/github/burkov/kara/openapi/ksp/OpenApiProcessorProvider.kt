package com.github.burkov.kara.openapi.ksp

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class OpenApiProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        kspLogger = environment.logger
        val outputDir = environment.options["out"]
        requireNotNull(outputDir) { "please specify openapi output directory" }
        return OpenApiProcessor(outputDir)
    }
}