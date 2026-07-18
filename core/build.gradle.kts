import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    // AGP 9+ provides Kotlin itself; no compose here, this module is pure logic + data-layer glue.
}

android {
    namespace = "style.xero.vibrateawake.core"
    compileSdk = 37

    defaultConfig {
        // Must be <= the lowest consumer (phone app minSdk 26).
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    // Exposed as `api` so both :app and :wear get the Data Layer types transitively;
    // the session read/write helpers in this module take/return DataMap.
    api(libs.play.services.wearable)
}
