plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "co.uk.doverguitarteacher.rubbishdayreminder"
    compileSdk = 36

    defaultConfig {
        applicationId = "co.uk.doverguitarteacher.rubbishdayreminder"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
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

    // Use Java 17 for current Compose + Material3
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
    // Compose BOM from version catalog
    implementation(platform(libs.androidx.compose.bom))

    // Core Compose libs (from BOM)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)

    // Foundation (remove the hard‑coded 1.6.7 version; BOM supplies the version)
    implementation("androidx.compose.foundation:foundation")

    // ***** IMPORTANT: Explicit Material3 1.2.1 for TimePicker APIs *****
    implementation("androidx.compose.material3:material3:1.2.1")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Google Mobile Ads
    implementation("com.google.android.gms:play-services-ads:23.0.0")

    // Fragment / Activity (fine to keep)
    implementation("androidx.fragment:fragment-ktx:1.8.6")
    implementation("androidx.activity:activity-ktx:1.9.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
