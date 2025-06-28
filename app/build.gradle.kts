plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.app_acvariu"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.app_acvariu"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.7"  // Use the latest stable version
    }
}

dependencies {

    implementation("com.airbnb.android:lottie-compose:6.0.0")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Retrofit for HTTP API calls
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    // Gson converter for JSON
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // OkHttp for lower-level network operations
    implementation("com.squareup.okhttp3:okhttp:4.9.3")

    // Logging interceptor (optional)
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")

    // Retrofit core
    implementation("com.squareup.retrofit2:retrofit:2.9.0")

    // OkHttp (if not already added)
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")

    // Scalars Converter for plain text responses
    implementation("com.squareup.retrofit2:converter-scalars:2.9.0")

    // Compose BOM (Bill of Materials) – helps align all Compose library versions
    implementation(platform("androidx.compose:compose-bom:2023.01.00"))

    // Core Compose libraries
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Activity Compose support
    implementation("androidx.activity:activity-compose:1.7.0")

    // Lifecycle ViewModel Compose support
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")


    // Lifecycle & ViewModel (for Compose)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
}


