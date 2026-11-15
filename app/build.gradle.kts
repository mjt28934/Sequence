import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("app.cash.paparazzi") version "1.3.5"
}

// Signing material never lives in the repository. Put these in local.properties (git-ignored)
// or provide them as environment variables in CI:
//   sequence.storeFile, sequence.storePassword, sequence.keyAlias, sequence.keyPassword
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun signingValue(key: String, env: String): String? =
    localProperties.getProperty(key) ?: System.getenv(env)

android {
    namespace = "com.sequence.watchorder"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "com.sequence.watchorder"
        minSdk = 26
        targetSdk = 35
        versionCode = 150
        versionName = "1.4"
    }

    signingConfigs {
        val storePath = signingValue("sequence.storeFile", "SEQUENCE_STORE_FILE")
        val storePasswordValue = signingValue("sequence.storePassword", "SEQUENCE_STORE_PASSWORD")
        if (storePath != null && storePasswordValue != null) {
            create("release") {
                storeFile = rootProject.file(storePath)
                storePassword = storePasswordValue
                keyAlias = signingValue("sequence.keyAlias", "SEQUENCE_KEY_ALIAS")
                keyPassword = signingValue("sequence.keyPassword", "SEQUENCE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = false
            signingConfigs.findByName("release")?.let { signingConfig = it }
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
        buildConfig = true
    }

    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}")
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")

    testImplementation("junit:junit:4.13.2")
}
