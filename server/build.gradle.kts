plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    // Runs only on the PC, so it can use the same JDK as Gradle.
    jvmToolchain(21)
}

dependencies {
    implementation(project(":protocol"))
    implementation(libs.jna)
    testImplementation(libs.kotlin.test)
}

tasks.test {
    useJUnitPlatform()
}
