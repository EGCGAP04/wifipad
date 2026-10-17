plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "io.github.egcgap04.wifipad.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.egcgap04.wifipad.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }
}

kotlin {
    // Same as :protocol.
    jvmToolchain(17)
}

dependencies {
    implementation(project(":protocol"))
    testImplementation(libs.kotlin.test.junit)
}
