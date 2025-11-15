import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("org.jetbrains.kotlin.kapt")
    id("com.google.gms.google-services")
}

android {
    namespace = "za.ac.iie.opsc_poe_screens"
    compileSdk = 36

    val keystorePropertiesFile = rootProject.file("keystore.properties")
    val keystoreProperties = Properties()
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))

    signingConfigs {
        // This is your existing release config - no changes here
        create("release") {
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
        }


        getByName("debug") {
            // Point to the debug.keystore file you just pasted into the app/ folder
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    defaultConfig {
        applicationId = "za.ac.iie.opsc_poe_screens"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // Debug builds should now use the debug key
            signingConfig = signingConfigs.getByName("debug")
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
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.lifecycle.viewmodel.android)
    implementation(libs.firebase.database)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("com.google.android.flexbox:flexbox:3.0.0")



    // 1. Add the Firebase Bill of Materials (BoM)
    // This is highly recommended. It manages the versions of all Firebase libraries
    // to ensure they are compatible with each other.
    val bom = platform("com.google.firebase:firebase-bom:33.1.1")
    implementation(bom)

    // 2. Add the specific Firebase libraries you need
    // We get access to services like Firestore and Authentication by adding them here.
    // The "-ktx" ending means they include special support for Kotlin.

    // For Firestore Database
    implementation("com.google.firebase:firebase-firestore-ktx")


    // 3. Add the Coroutines library for Google Play Services
    // This gives you the .await() function to write cleaner async code.
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    implementation("com.google.firebase:firebase-storage-ktx")

    //Glide for images
    implementation("com.github.bumptech.glide:glide:4.16.0")




    //Bar Chart
    //implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation(libs.mpandroidchart)

    /*
    val camerax_version = "1.4.2"
    implementation("androidx.camera:camera-core:$camerax_version")
    implementation("androidx.camera:camera-camera2:$camerax_version")
    implementation("androidx.camera:camera-lifecycle:$camerax_version")
    implementation("androidx.camera:camera-view:$camerax_version")
     */

    val camerax_version = "1.4.2"
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // Room components
    implementation(libs.androidx.room.runtime)
    kapt(libs.androidx.room.compiler)

// Kotlin Extensions and Coroutines support for Room
    implementation(libs.androidx.room.ktx)

    //Unchanged
    /*implementation("androidx.room:room-runtime:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")*/

}