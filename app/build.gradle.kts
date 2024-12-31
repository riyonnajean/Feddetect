plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.fed"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.fed"
        minSdk = 24
        targetSdk = 34
        versionCode = 2
        versionName = "1.1"
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

    sourceSets["main"].jniLibs.srcDirs("src/main/jniLibs")
}

dependencies {
    // Android dependencies
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.activity:activity-ktx:1.7.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Test dependencies
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // TensorFlow Lite dependencies
    implementation("org.tensorflow:tensorflow-lite:+") // Match version of TensorFlow Lite
    implementation("org.tensorflow:tensorflow-lite-support:+") {
        exclude(group = "com.amazon.ion") // Avoid unnecessary dependencies
    }

    // TensorFlow GPU support
    implementation("org.tensorflow:tensorflow-lite-gpu:+") // Ensure GPU version matches

    // Networking dependencies
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // APK Parsing
    implementation("net.dongliu:apk-parser:2.6.10")

    // Google ML Kit
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:16.2.0")
}
