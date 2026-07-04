// Root build file. Plugins are declared here (apply false) so that sub-modules
// can apply them by id; concrete configuration lives in build-logic convention
// plugins to keep each module's build script tiny and consistent.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
