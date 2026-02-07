pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "9.0.0"
        id("org.jetbrains.kotlin.android") version "2.2.10"
        id("com.google.devtools.ksp") version "2.3.2"
        id("org.jetbrains.kotlin.plugin.compose") version "2.2.10"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ScanSentry"
include(":app")
