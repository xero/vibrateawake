import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    // AGP 9+ provides Kotlin itself; only the Compose compiler plugin is applied here.
    alias(libs.plugins.kotlin.compose)
}

// Release signing shares the phone app's keystore.properties. Same key is required so the
// Data Layer pairs the two apps (they also share the applicationId).
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "style.xero.vibrateawake.wear"
    compileSdk = 37

    defaultConfig {
        // Same package as the phone app so the Data Layer connects them; shipped as a
        // separate artifact on the Wear release track with its own versionCode scheme.
        applicationId = "style.xero.vibrateawake"
        minSdk = 30
        targetSdk = 36
        // Wear lives in a 1000+ range so its versionCode never collides with the phone's
        // (1, 2, 3, ...). Google Play requires every APK under one listing to have a
        // unique versionCode, even across form factors.
        versionCode = 1001
        versionName = "1.0"
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = rootProject.file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)

    debugImplementation(libs.androidx.ui.tooling)
}
