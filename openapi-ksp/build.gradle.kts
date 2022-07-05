plugins {
    kotlin("jvm")
}

val kspVersion: String by project

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
    implementation("com.google.devtools.ksp:symbol-processing-api:$kspVersion")
    implementation("io.swagger.core.v3:swagger-core:2.2.0")
    implementation("io.swagger.core.v3:swagger-models:2.2.0")
    implementation("kara", "kara", "0.1.27")
    implementation(project(":baseapp"))
}

sourceSets.main {
    java.srcDirs("src/main/kotlin")
}