import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(sjy.plugins.buildlogic.multiplatform.app)
    alias(sjy.plugins.buildlogic.multiplatform.cmp)
    alias(libs.plugins.composeHotReload)
    alias(sjy.plugins.kotlin.serialization)
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
        commonMain.dependencies {
            // Compose Material3
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            
            // Ktor Client
            implementation(sjy.ktor.content.negotiation)
            implementation(sjy.ktor.serialization)
            
            // Project dependencies
            implementation(project(":ktoon-core"))
            implementation(project(":ktoon-ktor"))
            
            // Serialization
            implementation(sjy.kotlin.serialization)
        }
        
        androidMain.dependencies {
            implementation(sjy.ktor.okhttp)
        }
        
        iosMain.dependencies {
            implementation(sjy.ktor.darwin)
        }
        
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(sjy.ktor.cio)
        }
        
        commonTest.dependencies {
            implementation(libs.kotest.property)
            implementation(kotlin("test"))
        }
    }
}

android {
    namespace = "io.ktoon"
    defaultConfig {
        applicationId = "io.ktoon"
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}

compose.desktop {
    application {
        mainClass = "io.ktoon.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "io.ktoon"
            packageVersion = "1.0.0"
        }
    }
}
