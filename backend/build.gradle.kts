plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
}

group = "io.ktoon"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // Ktor Server
    implementation("io.ktor:ktor-server-core:3.3.3")
    implementation("io.ktor:ktor-server-netty:3.3.3")
    implementation("io.ktor:ktor-server-content-negotiation:3.3.3")
    
    // Serialization
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.3.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    
    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.15")
}

application {
    mainClass.set("io.ktoon.backend.ApplicationKt")
}
