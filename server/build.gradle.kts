plugins {
    alias(libs.plugins.kotlin.jvm)
    application
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

application {
    mainClass = "io.github.egcgap04.wifipad.server.MainKt"
    // JNA loads native code through JNI, which JDK 24+ warns about unless it is enabled.
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}

tasks.test {
    useJUnitPlatform()
}
