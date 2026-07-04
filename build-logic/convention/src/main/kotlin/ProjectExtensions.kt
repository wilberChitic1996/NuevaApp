import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

/**
 * Convenience accessor for the version catalog from within convention plugins.
 *
 * Deliberately NOT named `libs`: build-logic's classes are on the module
 * build-script classpath, so a `Project.libs` here would shadow Gradle's
 * generated type-safe `libs` accessor inside every module's `dependencies {}`
 * block, breaking `libs.androidx.…` references.
 */
val Project.versionCatalog: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun VersionCatalog.version(alias: String): String =
    findVersion(alias).get().requiredVersion

fun VersionCatalog.intVersion(alias: String): Int = version(alias).toInt()
