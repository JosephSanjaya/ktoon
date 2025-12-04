import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(sjy.plugins.buildlogic.multiplatform.lib)
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
            implementation(project(":ktoon-core"))
            implementation(sjy.kotlin.serialization)
            implementation("io.ktor:ktor-server-core:3.3.3")
            implementation("io.ktor:ktor-server-content-negotiation:3.3.3")
        }
        commonTest.dependencies {
            implementation(sjy.kotlin.test)
            implementation(sjy.coroutines.test)
            implementation("io.ktor:ktor-server-test-host:3.3.3")
            implementation("io.ktor:ktor-server-status-pages:3.3.3")
            implementation("io.ktor:ktor-server-compression:3.3.3")
            implementation("io.ktor:ktor-serialization-kotlinx-json:3.3.3")
            implementation(libs.kotest.property)
        }
        jvmMain.dependencies {
            implementation("io.ktor:ktor-server-netty:3.3.3")
            implementation("io.ktor:ktor-server-call-logging:3.3.3")
            implementation("io.ktor:ktor-server-status-pages:3.3.3")
            implementation("io.ktor:ktor-server-compression:3.3.3")
            implementation("io.ktor:ktor-serialization-kotlinx-json:3.3.3")
            implementation("ch.qos.logback:logback-classic:1.5.12")
        }
    }
}

android {
    namespace = "io.ktoon.ktor.server"
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
