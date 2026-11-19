// Module Kotlin pur : fabrique les codes USSD Yas/MVola (teste vite sur le PC)
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(libs.junit)
}
