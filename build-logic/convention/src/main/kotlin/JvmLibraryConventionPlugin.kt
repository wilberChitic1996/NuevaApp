import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * The pure-JVM library convention, used by :core:domain. No Android dependencies
 * whatsoever — this is what guarantees the game logic stays testable on the JVM
 * and reusable server-side for leaderboard score re-validation.
 */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("org.jetbrains.kotlin.jvm")
        }

        configureKotlinJvm()

        dependencies {
            add("testImplementation", versionCatalog.findLibrary("junit").get())
        }
    }
}
