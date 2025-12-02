import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(sjy.plugins.buildlogic.multiplatform.lib)
    alias(sjy.plugins.buildlogic.multiplatform.cmp)
    alias(libs.plugins.composeHotReload)
}

kotlin {
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
android {
    namespace = "io.ktoon.core"
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
