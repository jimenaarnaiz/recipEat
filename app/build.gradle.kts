import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
    id("com.google.devtools.ksp")
    id ("jacoco") //para coverage
}


val localProperties = Properties()
localProperties.load(FileInputStream(rootProject.file("local.properties")))

val spoonacularKey: String = localProperties.getProperty("API_SPOONACULAR_KEY")
val sonarqubeToken: String = localProperties.getProperty("SONARQUBE_TOKEN")
val geminiKey: String = localProperties.getProperty("API_GEMINI_KEY")


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

        //API KEYS
        buildConfigField("String", "API_SPOONACULAR_KEY", spoonacularKey)
        buildConfigField("String", "SONARQUBE_TOKEN", sonarqubeToken)
        buildConfigField("String", "API_GEMINI_KEY", geminiKey)

    }

    //para tests
    testOptions {
        unitTests.all {
            it.extensions.configure<JacocoTaskExtension> {
                isIncludeNoLocationClasses = true
                excludes = listOf("jdk.internal.*") // recomendado por Gradle
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        getByName("debug") {
            enableUnitTestCoverage = true // permite cobertura en variantes de build
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
        buildConfig = true  // Habilita BuildConfig para lo de las key
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
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.generativeai) //navigation
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    //new
    //para imágenes
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
    implementation(libs.androidx.room.runtime)
    // Room compiler (para que Room genere las implementaciones necesarias)
    ksp(libs.androidx.room.compiler)// Esto es necesario si usas Kotlin
    // Si usas coroutines con Room
    implementation(libs.androidx.room.ktx)
    testImplementation(libs.mockk) //para mockear test
    //org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3
    testImplementation(libs.kotlinx.coroutines.test) //para corrutinas en tests


}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest") // Asegura que se ejecuten los tests primero

    reports {
        xml.required.set(true) //necesario para sonarqube
        html.required.set(true)
    }

    val fileFilter = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "**/Hilt*.*",
        "**/di/**"
    )

    val buildDirPath = layout.buildDirectory.asFile.get()

    val classDirs = fileTree(buildDirPath.resolve("tmp/kotlin-classes/debug")) {
        exclude(fileFilter)
    }

    classDirectories.setFrom(classDirs)
    sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))
    executionData.setFrom(files(buildDirPath.resolve("jacoco/testDebugUnitTest.exec")))
}