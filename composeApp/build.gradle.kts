import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(sjy.plugins.buildlogic.multiplatform.app)
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
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
        commonMain.dependencies {

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
