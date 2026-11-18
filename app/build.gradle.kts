plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.unsa.dhh.captions"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.unsa.dhh.captions"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        // libvosk.so pesa ~10 MB por ABI: solo teléfonos ARM (Tecno Spark 20 = arm64) y el emulador x86_64
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
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
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.04.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Jetpack Compose UI
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Room DB
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

    // JSON Parsing (para carga de diccionarios MINEDU)
    implementation("com.google.code.gson:gson:2.10.1")

    // Módulo 2: ASR local Vosk (Kaldi) con confianza por palabra. JNA debe ir como AAR (trae las .so).
    implementation("com.alphacephei:vosk-android:0.3.75")
    implementation("net.java.dev.jna:jna:5.18.1@aar")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

// ---------------------------------------------------------------------------------------------------------------
// Modelo Vosk (≈40 MB, no se versiona en git). Sin él la app compila pero el motor Vosk no puede arrancar,
// así que se detiene la compilación con instrucciones. Descarga: scripts/descargar_modelo_vosk.sh
// ---------------------------------------------------------------------------------------------------------------
val voskModelDir = file("src/main/assets/vosk-model-es")
val checkVoskModel by tasks.registering {
    val required = listOf("am/final.mdl", "graph/HCLr.fst", "graph/Gr.fst", "conf/model.conf", "uuid").map { File(voskModelDir, it) }
    inputs.files(required).optional()
    doLast {
        val missing = required.filterNot { it.exists() }
        if (missing.isNotEmpty()) {
            throw GradleException(
                "Falta el modelo Vosk en app/src/main/assets/vosk-model-es (${missing.joinToString { it.name }}). " +
                    "Ejecuta: sh scripts/descargar_modelo_vosk.sh"
            )
        }
    }
}
tasks.named("preBuild") { dependsOn(checkVoskModel) }
