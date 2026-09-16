plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.gentlefin.wallpaper"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.gentlefin.wallpaper"
        minSdk = 24 // Android 7.0+ - CATATAN: laporan bug WebGL-di-wallpaper
                    // pernah muncul justru di Android 7, jadi minSdk ini
                    // mungkin perlu dinaikkan tergantung hasil testing kamu.
        targetSdk = 34
        versionCode = 1
        versionName = "0.1-dev"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
}
