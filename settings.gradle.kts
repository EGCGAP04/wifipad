pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    // The version catalog is not available in the settings plugins block, so the
    // version goes here. It downloads the JDK 17 required by jvmToolchain(17).
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
    }
}

rootProject.name = "wifipad"

include(":protocol")
