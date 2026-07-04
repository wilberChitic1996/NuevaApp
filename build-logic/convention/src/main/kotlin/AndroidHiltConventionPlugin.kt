import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Applies Hilt + KSP to a module and wires the compiler. Kept separate from the
 * library/application plugins so pure-logic Android modules can opt out of DI.
 */
class AndroidHiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("com.google.devtools.ksp")
            apply("com.google.dagger.hilt.android")
        }

        dependencies {
            add("implementation", versionCatalog.findLibrary("hilt-android").get())
            add("ksp", versionCatalog.findLibrary("hilt-compiler").get())
        }
    }
}
