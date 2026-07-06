pluginManagement {
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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AiJobHunter"
include(":app")
 
include(":core:model")
include(":core:common")
include(":core:database")
include(":core:network")
include(":core:ai")
include(":domain")
include(":core:data")
include(":navigation")
include(":feature:shared_ui")
include(":feature:auth")
include(":feature:profile")
include(":feature:feed")
include(":feature:tracker")
