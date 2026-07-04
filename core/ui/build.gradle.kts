plugins {
    alias(libs.plugins.cadejo.android.library)
    alias(libs.plugins.cadejo.android.compose)
}

android {
    namespace = "gt.guardian.cadejo.core.ui"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(libs.androidx.core.ktx)
}
