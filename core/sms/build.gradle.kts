// Module Kotlin pur (pas Android) : le parseur se teste vite sur le PC
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(libs.junit)
}
