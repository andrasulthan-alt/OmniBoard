plugins {
    // AGP 9 has built-in Kotlin: do NOT add org.jetbrains.kotlin.android
    id("com.android.application") version "9.4.0"
}

android {
    namespace = "io.github.andrasulthan.omniboard"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.andrasulthan.omniboard"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "0.2.0"
    }

    signingConfigs {
        create("release") {
            val ks = System.getenv("KEYSTORE_FILE")
            if (ks != null) {
                storeFile = file(ks)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = if (System.getenv("KEYSTORE_FILE") != null)
                signingConfigs.getByName("release")
            else
                signingConfigs.getByName("debug")
        }
    }

    // No Google-signed dependency metadata blob in the APK
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}
