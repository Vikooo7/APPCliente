plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)   // lee google-services.json (proyecto rutalog-peru)
}

android {
    namespace = "com.example.rutalogcliente"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.rutalogcliente"
        minSdk = 29
        targetSdk = 37
        versionCode = 1
        // URL de la API: la de Vercel, o otra al compilar con -PurlApi=http://localhost:3100/ (pruebas en la PC).
        buildConfigField("String", "URL_API", "\"" + (providers.gradleProperty("urlApi").orNull ?: "https://app-api-rutalog.vercel.app/") + "\"")
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// Room guarda el esquema de cada versión en app/schemas: sirve para revisar las migraciones.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // Inicio con Google: Credential Manager entrega la cuenta y Firebase emite el token que valida la API
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    // Notificaciones push en tiempo real: la API avisa cuando el courier avanza el envío
    implementation(libs.firebase.messaging)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // QR de la guía para que el courier la escanee al recoger (HU-C02)
    implementation(libs.zxing.core)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Room: base de datos local (tablas usuarios y envios)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // API REST: Retrofit + Gson
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)

    // Sincronización en segundo plano y sesión cifrada
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.security.crypto)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}