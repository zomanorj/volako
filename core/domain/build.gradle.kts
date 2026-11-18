// Regles metier : conversion SMS -> operation, calcul des soldes (Kotlin pur, teste vite)
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:sms"))
    testImplementation(libs.junit)
}
