import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Load keystore.properties if present (takes priority over env vars)
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties()
val keystoreAvailable: Boolean = when {
    keystorePropsFile.exists() -> {
        keystoreProps.load(FileInputStream(keystorePropsFile))
        true
    }
    System.getenv("KEYSTORE_PATH") != null && System.getenv("KEYSTORE_PASSWORD") != null -> true
    else -> {
        println("INFO: keystore.properties not found and KEYSTORE_PATH env var not set.")
        println("INFO: Release builds will not be signed with a release key.")
        println("INFO: See keystore.properties.example to set up release signing.")
        false
    }
}

android {
    namespace = "com.bankyar"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.bankyar"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "1.5"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (keystorePropsFile.exists()) {
                storeFile = file(keystoreProps.getProperty("storeFile") ?: "release-key.jks")
                storePassword = keystoreProps.getProperty("storePassword") ?: ""
                keyAlias = keystoreProps.getProperty("keyAlias") ?: ""
                keyPassword = keystoreProps.getProperty("keyPassword") ?: ""
            } else {
                storeFile = file(System.getenv("KEYSTORE_PATH") ?: "release-key.jks")
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
                keyAlias = System.getenv("KEY_ALIAS") ?: ""
                keyPassword = System.getenv("KEY_PASSWORD") ?: ""
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystoreAvailable) {
                signingConfig = signingConfigs.getByName("release")
            }
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.biometric:biometric:1.1.0")
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
