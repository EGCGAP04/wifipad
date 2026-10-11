plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    // Also used by :app: stick to Java 17 and to APIs available on Android API 26.
    jvmToolchain(17)
}

dependencies {
    testImplementation(libs.kotlin.test)
}

tasks.test {
    useJUnitPlatform()
}
