plugins {
    alias(libs.plugins.cadejo.android.library)
    alias(libs.plugins.cadejo.android.compose)
    alias(libs.plugins.cadejo.android.hilt)
}

android {
    namespace = "gt.guardian.cadejo.feature.daily"
}

// Daily challenge: today's global-seed puzzle plus the server-validated leaderboard.
// Reuses the game board/controls from :feature:game so play is identical.
dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:ui"))
    implementation(project(":feature:game"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)
}
