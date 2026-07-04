plugins {
    alias(libs.plugins.cadejo.android.application)
    alias(libs.plugins.cadejo.android.compose)
    alias(libs.plugins.cadejo.android.hilt)
}

android {
    namespace = "gt.guardian.cadejo"

    defaultConfig {
        applicationId = "gt.guardian.cadejo"
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
        getByName("release") {
            // R8: shrink + obfuscate + optimise. Full mode is enabled in
            // gradle.properties. Release signing is wired in Phase 6.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    implementation(project(":feature:game"))
    implementation(project(":feature:meta"))
    implementation(project(":feature:daily"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.timber)
}
