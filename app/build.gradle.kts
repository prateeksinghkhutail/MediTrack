plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.team8.meditrack"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.team8.meditrack"
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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.cardview)
    implementation(libs.org.eclipse.paho.client.mqttv3)
    implementation(libs.org.eclipse.paho.android.service)
    implementation(libs.legacy.support.v4)
    implementation(libs.work.runtime)
    implementation(libs.json)

    // Add the AndroidX LocalBroadcastManager dependency
    implementation(libs.localbroadcastmanager)

    // Google Maps dependencies
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)

    // Room database dependencies for local storage
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    // RecyclerView
    implementation(libs.recyclerview)

    implementation(libs.fragment)
}