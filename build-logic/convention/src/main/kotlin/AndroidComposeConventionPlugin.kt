import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Enables Jetpack Compose for a module. Applies the Kotlin Compose compiler
 * plugin (Kotlin 2.0+ ships the Compose compiler as a first-party plugin) and
 * wires the Compose BOM so every Compose artifact is version-aligned.
 *
 * Works for both library and application modules: Gradle registers the concrete
 * extension type (LibraryExtension / ApplicationExtension), so we look those up
 * and treat whichever is present as the shared CommonExtension supertype.
 */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        val extension: CommonExtension<*, *, *, *, *, *> =
            extensions.findByType(LibraryExtension::class.java)
                ?: extensions.findByType(ApplicationExtension::class.java)
                ?: error("cadejo.android.compose must be applied after an Android library/application plugin")
        extension.buildFeatures.compose = true

        dependencies {
            val bom = platform(versionCatalog.findLibrary("androidx-compose-bom").get())
            add("implementation", bom)
            add("androidTestImplementation", bom)

            add("implementation", versionCatalog.findLibrary("androidx-compose-ui").get())
            add("implementation", versionCatalog.findLibrary("androidx-compose-ui-graphics").get())
            add("implementation", versionCatalog.findLibrary("androidx-compose-ui-tooling-preview").get())
            add("implementation", versionCatalog.findLibrary("androidx-compose-material3").get())

            add("debugImplementation", versionCatalog.findLibrary("androidx-compose-ui-tooling").get())
            add("debugImplementation", versionCatalog.findLibrary("androidx-compose-ui-test-manifest").get())

            add("androidTestImplementation", versionCatalog.findLibrary("androidx-compose-ui-test-junit4").get())
        }
    }
}
