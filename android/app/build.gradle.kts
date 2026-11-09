import java.util.Properties
import org.gradle.api.tasks.Exec

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.google.services)
    alias(libs.plugins.crashlytics)
}

/**
 * URL de la API para builds de desarrollo.
 *
 * Por defecto usa `127.0.0.1` porque el script `scripts/prepare-dev.sh` configura
 * `adb reverse tcp:8080 tcp:8080`, lo que funciona tanto en emulador como en un
 * teléfono físico conectado por USB.
 *
 * Se puede sobreescribir en `android/local.properties` o con
 * `-Pshoppi.apiBaseUrl=http://192.168.x.x:8080/api/`.
 */
val localProperties = Properties().apply {
    val archivo = rootProject.file("local.properties")
    if (archivo.exists()) {
        archivo.inputStream().use { load(it) }
    }
}

fun propiedadLocal(nombre: String, porDefecto: String): String =
    localProperties.getProperty(nombre)
        ?: providers.gradleProperty(nombre).orNull
        ?: porDefecto

val apiBaseUrlDebug = propiedadLocal(
    "shoppi.apiBaseUrl",
    "http://127.0.0.1:8080/api/",
)

android {
    namespace = "com.shoppi"

    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.shoppi"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8080/api/\"")
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrlDebug\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    // Room: fuente de verdad local.
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Sincronización en segundo plano.
    implementation(libs.work.runtime)

    // Serialización de ítems y DTOs.
    implementation(libs.gson)

    // DAO remoto (API REST).
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)

    implementation(libs.coroutines.android)

    // UI: Compose + Navigation + ViewModel + imágenes.
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.coil.compose)

    // Firebase auxiliar: push, imágenes, config remota y observabilidad.
    // El BoM fija las versiones; no se declaran versiones por dependencia.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.config)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.storage)

    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
}

/**
 * Prepara el entorno de desarrollo antes de compilar/instalar el APK de debug:
 * levanta la API local si no está corriendo y configura `adb reverse` para el
 * dispositivo conectado. Así "Run" en Android Studio es suficiente para probar
 * el flujo completo sin arrancar el backend en otra terminal.
 */
val prepareDevEnvironment by tasks.registering(Exec::class) {
    group = "shoppi"
    description = "Levanta la API local y configura adb reverse para el dispositivo."
    workingDir = rootProject.projectDir
    commandLine("bash", rootProject.file("scripts/prepare-dev.sh").absolutePath)
    standardOutput = System.out
    errorOutput = System.err
}

val stopDevBackend by tasks.registering(Exec::class) {
    group = "shoppi"
    description = "Detiene la API local iniciada por prepareDevEnvironment."
    workingDir = rootProject.projectDir
    commandLine("bash", rootProject.file("scripts/stop-dev.sh").absolutePath)
    isIgnoreExitValue = true
}

tasks.matching { it.name == "assembleDebug" || it.name == "installDebug" }.configureEach {
    dependsOn(prepareDevEnvironment)
}
