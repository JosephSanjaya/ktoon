import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(sjy.plugins.buildlogic.multiplatform.lib)
    alias(sjy.plugins.buildlogic.multiplatform.cmp)
    alias(sjy.plugins.kotlin.serialization)
    alias(libs.plugins.composeHotReload)
}

kotlin {
    android {
        namespace = "io.ktoon.core"
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
            implementation(sjy.kotlin.serialization)
        }
        commonTest.dependencies {
            implementation(sjy.kotlin.test)
        }
        webMain.dependencies {
            implementation(libs.kotlinx.browser)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(sjy.ktor.cio)
        }
    }
}

ktorfit {
    compilerPluginVersion.set("2.3.3")
}
