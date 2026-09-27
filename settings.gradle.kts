pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        google()
        mavenCentral()
        maven("https://redirector.kotlinlang.org/maven/compose-dev")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "Echo"
include(":client")
include(":apps:android")
include(":apps:desktop")
include(":apps:ios")
include(":apps:web")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")