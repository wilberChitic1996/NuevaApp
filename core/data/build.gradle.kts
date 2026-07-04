plugins {
    alias(libs.plugins.cadejo.android.library)
    alias(libs.plugins.cadejo.android.hilt)
}

android {
    namespace = "gt.guardian.cadejo.core.data"
}

// :core:data implements the interfaces declared by :core:domain and owns all the
// framework-touching concerns (persistence, randomness, networking). Phase 1 only
// needs the seed source; Room + DataStore + the signed save arrive in Phase 3.
dependencies {
    implementation(project(":core:domain"))
}
