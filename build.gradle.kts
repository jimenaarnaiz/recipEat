// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.gms.google.services) apply false
    id("org.sonarqube") version "3.3" apply true
    id("com.google.devtools.ksp") version "2.0.21-1.0.27" apply false

}

// Lee el archivo local.properties
val localProperties = rootProject.file("local.properties")
val properties = java.util.Properties()
properties.load(localProperties.inputStream())

// Obtén el token
val sonarToken: String = properties.getProperty("SONARQUBE_TOKEN")

sonarqube {
    properties {
        property("sonar.host.url", "http://localhost:9000") // Actualiza con la URL correcta de tu servidor
        property("sonar.projectKey", "com.example.recipeat")  // Mi proyecto
        property("sonar.projectName", "recipEat")
        property("sonar.projectVersion", "1.0")
        property("sonar.token", sonarToken)
        property("sonar.sources", listOf("src/main/java"))  // O la ruta correspondiente a mi código
        property("sonar.tests", "src/androidTest/java")  // Rutas de tests
        property("sonar.java.binaries", "build/intermediates/classes/debug")  // Compilados del código
    }
}


