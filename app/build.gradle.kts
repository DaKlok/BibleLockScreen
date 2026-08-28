plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    namespace = "com.daklok.biblelockscreen"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.daklok.biblelockscreen"
        minSdk = 24
        targetSdk = 36
        versionCode = 13
        versionName = "2.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Jsoup - na získanie verša z YouVersion stránky
    implementation("org.jsoup:jsoup:1.17.2")

    // WorkManager - na spúšťanie úlohy každý deň
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Coil - na načítanie obrázkov v UI
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Gson - na ukladanie objektov (ak by bolo treba)
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.32.0")

    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.material.icons.extended) // Add this line
}