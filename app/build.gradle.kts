plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.ksp)
    // add the kotlinx serialization plugin to handle the json data
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.21"


    id("com.google.gms.google-services")
}

android {
    namespace = "com.faiz.trekandtrack"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.faiz.trekandtrack"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.1"

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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }
}

dependencies {
    // AndroidX & Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Jetpack Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Room Database
    implementation(libs.bundles.androidx.room)
    ksp(libs.androidx.room.compiler)

    // DataStore for preferences
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Coil - Image Loading Library for Compose
    implementation("io.coil-kt:coil-compose:2.5.0")

    //----Additions for networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0") // The core library for making network requests to your backend
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0") // The library for converting Kotlin objects to and from JSON.
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0") // A converter that allows retrofit to use kotlinx Serialization
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0") // A utility to log network request and response details, which is very helpful for debugging

    // Moshi for JSON parsing (for exchange rate API)
    implementation("com.squareup.moshi:moshi:1.15.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.9.0")
    ksp("com.squareup.moshi:moshi-kotlin-codegen:1.15.0")

    //splash screen - removed the androidx.core:core-splashscreen library to avoid drawable issues
    //implementation("androidx.core:core-splashscreen:1.0.1")

    //Apache POI for Excel export
    implementation("org.apache.poi:poi:5.2.3")
    implementation("org.apache.poi:poi-ooxml:5.2.3")

    //For advanced pdfs
    //implementation("com.itextpdf:itext7-core:7.2.5")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)


    // Firebase BOM - manages all Firebase versions
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")

// Google Sign-In
    implementation("com.google.android.gms:play-services-auth:20.7.0")
}