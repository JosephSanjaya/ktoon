import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(sjy.plugins.buildlogic.multiplatform.lib)
    alias(sjy.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "io.ktoon.ktor.server"
    }
    jvm()
    js {
        browser()
        binaries.executable()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":ktoon-core"))
            implementation(sjy.kotlin.serialization)
            implementation(libs.ktor.server.core)
            implementation(libs.ktor.ktor.server.content.negotiation)
        }
        commonTest.dependencies {
            implementation(sjy.kotlin.test)
            implementation(sjy.coroutines.test)
            implementation(libs.ktor.ktor.server.test.host)
            implementation(libs.ktor.server.status.pages)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotest.property)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.server.netty)
            implementation(libs.ktor.server.call.logging)
            implementation(libs.ktor.server.status.pages)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.logback.classic)
        }
    }
}

ktorfit {
    compilerPluginVersion.set("2.3.3")
}
