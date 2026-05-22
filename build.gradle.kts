// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.devtools.ksp)
}

android {
    namespace = "com.example.galaxytheme"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.galaxytheme"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}
