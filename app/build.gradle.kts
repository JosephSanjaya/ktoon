plugins {
    alias(sjy.plugins.buildlogic.app)
    alias(sjy.plugins.buildlogic.compose)
    alias(sjy.plugins.buildlogic.test)
}

android {
    namespace = "io.ktoon"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "io.ktoon"
        versionCode = libs.versions.version.code.get().toInt()
        versionName = libs.versions.version.name.get()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
