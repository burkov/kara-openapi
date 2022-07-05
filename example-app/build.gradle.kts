plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
}

ksp {
    arg("app", "example-app")
}

repositories {
    mavenCentral()
    maven("https://repo.labs.intellij.net/intdev") {
        metadataSources {
            mavenPom()
            artifact()
        }
    }
}

dependencies {
    implementation("org.slf4j:slf4j-simple:1.7.36")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.2")
    implementation("org.apache.logging.log4j:log4j:2.17.2")
    implementation("kara", "kara", "0.1.27")
    implementation("kara", "kara-exec", "0.1.27")
    implementation("io.swagger.core.v3:swagger-core:2.2.0")
    implementation("io.swagger.core.v3:swagger-models:2.2.0")
    implementation("com.github.kittinunf.fuel:fuel:2.3.1")
    implementation("com.github.kittinunf.fuel:fuel-jackson:2.3.1")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.6.21")
    ksp(project(":openapi-ksp"))
    implementation(project(":baseapp"))
    testImplementation(kotlin("test"))
}