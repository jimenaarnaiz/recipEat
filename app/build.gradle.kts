plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.example.recipeat"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.recipeat"
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
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.firebase.database)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.androidx.runtime.livedata)
    implementation(libs.material)
    implementation(libs.androidx.tv.material)
    implementation(libs.androidx.storage)
    implementation(libs.androidx.work.runtime.ktx) //navigation
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    //new
    //para imaágenes
    implementation(libs.coil.compose)
    //para poder usar viewModel()
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    //solicitudes HTTP con Retrofit
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor)
    //más icons
    implementation(libs.androidx.material.icons.extended)
    // Room - room_version = "2.6.1"
    val room_version = "2.6.1"
    implementation(libs.androidx.room.runtime)
    // Room compiler (para que Room genere las implementaciones necesarias)
    ksp(libs.androidx.room.compiler)// Esto es necesario si usas Kotlin
    // Si usas coroutines con Room
    implementation(libs.androidx.room.ktx)  // Opcional, pero útil si usas coroutines

}