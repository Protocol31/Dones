plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.dones.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.dones.app"
        minSdk = 26
        targetSdk = 35
        // En GitHub, cada compilación sube el número, así el celular la instala como actualización.
        versionCode = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionName = "0.2-demo"
    }

    // Llave fija para el demo: así cada APK nuevo se instala encima del anterior sin borrar los perfiles.
    // (Es una llave de prueba pública; para Google Play se usará otra, privada.)
    signingConfigs {
        getByName("debug") {
            storeFile = file("demo-debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.barcode.scanning)

    implementation(libs.kotlinx.serialization.json)
    // Código QR del certificado (solo genera; la lectura sigue con ML Kit).
    implementation("com.google.zxing:core:3.5.3")
    implementation(libs.kotlinx.coroutines.android)
}
