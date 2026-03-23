import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(sjy.plugins.buildlogic.multiplatform.lib)
    alias(sjy.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "io.ktoon.ktor"
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
            implementation(sjy.ktor.cio)
            implementation(sjy.ktor.content.negotiation)
            implementation(sjy.ktor.serialization)
        }
        commonTest.dependencies {
            implementation(sjy.kotlin.test)
            implementation(sjy.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
    }
}


ktorfit {
    compilerPluginVersion.set("2.3.3")
}
