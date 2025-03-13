plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.smartlockersolution"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.smartlockersolution"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        vectorDrawables.useSupportLibrary = true

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

    testOptions {
        unitTests {
            isIncludeAndroidResources = true

        }
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("androidx.camera:camera-core:1.3.0")
    implementation("androidx.camera:camera-camera2:1.3.0")
    implementation("androidx.camera:camera-lifecycle:1.3.0")
    implementation("androidx.camera:camera-view:1.3.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation ("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation ("com.google.zxing:core:3.5.2")
    implementation ("com.google.guava:guava:31.1-android")

    testImplementation ("org.mockito:mockito-core:3.12.4")

    // Explicitly include powermock-core once.
    testImplementation ("org.powermock:powermock-core:2.0.9")

    // Exclude powermock-core from these so it is not pulled in twice.
    testImplementation("org.powermock:powermock-module-junit4:2.0.9") {
        exclude(group = "org.powermock", module = "powermock-core")
    }
    testImplementation("org.powermock:powermock-api-mockito2:2.0.9") {
        exclude(group = "org.powermock", module = "powermock-core")
    }

    testImplementation ("androidx.test:core:1.4.0")
    testImplementation ("androidx.test.ext:junit:1.1.3")
    testImplementation ("org.robolectric:robolectric:4.8.1")
    testImplementation ("junit:junit:4.13.2")


    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}