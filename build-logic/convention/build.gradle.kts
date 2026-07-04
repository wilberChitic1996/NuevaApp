import org.gradle.api.JavaVersion

plugins {
    `kotlin-dsl`
}

group = "gt.guardian.cadejo.buildlogic"

// Convention plugins are compiled against JDK 17 bytecode; the Gradle daemon
// itself runs on JDK 21 in this environment, both are supported by AGP 8.9.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
}

// Register each convention plugin so modules can apply them by id.
gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "cadejo.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "cadejo.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "cadejo.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("androidHilt") {
            id = "cadejo.android.hilt"
            implementationClass = "AndroidHiltConventionPlugin"
        }
        register("jvmLibrary") {
            id = "cadejo.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
    }
}
