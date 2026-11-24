plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.endless.controller"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.endless.controller"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    // WebSocket ke liye (Internet connection)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // QR Scan karne ke liye
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    // JSON banane ke liye (jo hum server ko bhejenge)
    implementation("com.google.code.gson:gson:2.10.1")
}