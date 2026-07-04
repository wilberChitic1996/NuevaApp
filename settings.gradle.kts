pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Cadejo"

// --- Core modules -----------------------------------------------------------
include(":app")
include(":core:domain")
include(":core:data")
include(":core:ui")

// --- Feature modules --------------------------------------------------------
include(":feature:game")
include(":feature:meta")
include(":feature:daily")
