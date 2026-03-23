plugins {
    alias(sjy.plugins.buildlogic.app)
    alias(sjy.plugins.buildlogic.compose)
}

android {
    namespace = "io.ktoon"
    defaultConfig {
        applicationId = "io.ktoon"
        versionCode = 1
        versionName = "1.0"
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

dependencies {
    implementation(projects.shared)
}

ktorfit {
    compilerPluginVersion.set("2.3.3")
}
