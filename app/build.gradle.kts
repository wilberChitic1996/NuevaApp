import java.util.Properties

plugins {
    alias(libs.plugins.cadejo.android.application)
    alias(libs.plugins.cadejo.android.compose)
    alias(libs.plugins.cadejo.android.hilt)
}

// Read sensitive config from local.properties (git-ignored) or, in CI, from
// environment variables. NEVER hardcoded, NEVER committed.
val secrets = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun secret(key: String): String = (secrets.getProperty(key) ?: System.getenv(key) ?: "")

android {
    namespace = "gt.guardian.cadejo"

    defaultConfig {
        applicationId = "gt.guardian.cadejo"
        versionCode = 1
        versionName = "0.1.0"

        // Injected into BuildConfig at build time; blank in dev => features that
        // need them (leaderboard) simply stay disabled.
        buildConfigField("String", "SUPABASE_URL", "\"${secret("SUPABASE_URL")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${secret("SUPABASE_ANON_KEY")}\"")
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
