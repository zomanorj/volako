plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "mg.volako.core.data"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    api(project(":core:model"))
    api(project(":core:domain"))
    implementation(project(":core:database"))
    implementation(project(":core:sms"))
    api(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.datastore.preferences)

    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
}
