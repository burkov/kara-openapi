import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.20"
}

group = "org.example"
version = "1.0-SNAPSHOT"

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
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}