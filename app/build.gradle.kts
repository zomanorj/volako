plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "mg.volako"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "mg.volako"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 22
        versionName = "1.11.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:sms"))
    implementation(project(":core:ussd"))
    implementation(project(":core:data"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.kotlinx.serialization.core)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}
