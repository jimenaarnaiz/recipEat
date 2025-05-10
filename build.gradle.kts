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

//  token
val sonarToken: String = properties.getProperty("SONARQUBE_TOKEN")

sonarqube {
    properties {
        property("sonar.host.url", "https://sonarcloud.io") // URL del servidor
        property("sonar.projectKey", "jimenaarnaiz_recipEat")
        property("sonar.organization", "recipeat")
        property("sonar.token", sonarToken)
        property("sonar.sources", "src/main/kotlin")  // ruta correspondiente a mi código
        property("sonar.coverage.jacoco.xmlReportPaths", "app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml")
    }
}


