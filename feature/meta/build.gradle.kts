plugins {
    alias(libs.plugins.cadejo.android.library)
    alias(libs.plugins.cadejo.android.compose)
}

android {
    namespace = "gt.guardian.cadejo.feature.meta"
}

// Meta-progression (coin shop, permanent unlocks, HMAC-signed save). Scaffolded
// now so the module graph is complete; fleshed out in Phase 3.
dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:ui"))
}
