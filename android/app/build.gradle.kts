plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
}

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
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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

    testImplementation(libs.junit)
}
