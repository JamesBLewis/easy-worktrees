pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "easy-worktrees"

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}
