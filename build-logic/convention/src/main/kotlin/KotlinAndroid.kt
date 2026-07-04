import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/**
 * Shared Android + Kotlin configuration applied to both the application and every
 * Android library module. Centralising it here means bumping compileSdk or the
 * JVM target is a one-line change instead of an edit across seven build files.
 */
internal fun Project.configureKotlinAndroid(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    commonExtension.apply {
        compileSdk = versionCatalog.intVersion("compileSdk")

        defaultConfig {
            minSdk = versionCatalog.intVersion("minSdk")
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }

    extensions.configure<KotlinAndroidProjectExtension> {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
            // Treat all warnings as intentional; we keep the domain warning-clean.
            freeCompilerArgs.add("-Xconsistent-data-class-copy-visibility")
        }
    }
}

/** Configuration for the pure-JVM domain module (no Android dependencies). */
internal fun Project.configureKotlinJvm() {
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    extensions.configure<KotlinJvmProjectExtension> {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }
}
