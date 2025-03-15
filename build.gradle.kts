// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.gms.google.services) apply false
    id("org.sonarqube") version "3.3" apply true
}

sonarqube {
    properties {
        property("sonar.projectKey", "com.example.recipeat")  // Ajusta esto a tu propio proyecto
        property("sonar.projectName", "recipEat")
        property("sonar.projectVersion", "1.0")
        property("sonar.token", "squ_09a517edb1ff9b3da62d7947a66f5b94fe80b5db")
        property("sonar.sources", listOf("src/main/java"))  // O la ruta correspondiente a tu código
        property("sonar.tests", "src/androidTest/java")  // Rutas de tests
        property("sonar.java.binaries", "build/intermediates/classes/debug")  // Compilados del código
    }
}

