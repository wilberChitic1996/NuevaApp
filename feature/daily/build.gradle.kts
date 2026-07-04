plugins {
    alias(libs.plugins.cadejo.android.library)
    alias(libs.plugins.cadejo.android.compose)
}

android {
    namespace = "gt.guardian.cadejo.feature.daily"
}

// Daily challenge + leaderboard. Scaffolded now; the global-seed puzzle and the
// server-validated score submission arrive in Phase 4.
dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:ui"))
}
