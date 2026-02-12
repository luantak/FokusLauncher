import org.gradle.api.GradleException

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.compose.compiler)
}

val releaseKeystorePath = providers.environmentVariable("ANDROID_KEYSTORE_PATH")
        .orElse(providers.gradleProperty("ANDROID_KEYSTORE_PATH"))
        .orNull
val releaseKeystorePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD")
        .orElse(providers.gradleProperty("ANDROID_KEYSTORE_PASSWORD"))
        .orNull
val releaseKeyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS")
        .orElse(providers.gradleProperty("ANDROID_KEY_ALIAS"))
        .orNull
val releaseKeyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD")
        .orElse(providers.gradleProperty("ANDROID_KEY_PASSWORD"))
        .orNull
val hasReleaseSigning = !releaseKeystorePath.isNullOrBlank() &&
        !releaseKeystorePassword.isNullOrBlank() &&
        !releaseKeyAlias.isNullOrBlank() &&
        !releaseKeyPassword.isNullOrBlank()
val missingReleaseSigningInputs = buildList {
    if (releaseKeystorePath.isNullOrBlank()) add("ANDROID_KEYSTORE_PATH")
    if (releaseKeystorePassword.isNullOrBlank()) add("ANDROID_KEYSTORE_PASSWORD")
    if (releaseKeyAlias.isNullOrBlank()) add("ANDROID_KEY_ALIAS")
    if (releaseKeyPassword.isNullOrBlank()) add("ANDROID_KEY_PASSWORD")
}
val isCi = providers.environmentVariable("CI").orNull == "true"
val isReleaseTaskRequested = gradle.startParameter.taskNames.any {
    it.contains("release", ignoreCase = true)
}

if (isCi && isReleaseTaskRequested && missingReleaseSigningInputs.isNotEmpty()) {
    throw GradleException(
            "Release signing is required in CI. Missing: ${missingReleaseSigningInputs.joinToString(", ")}"
    )
}

android {
    namespace = "com.lu4p.fokuslauncher"
    compileSdk { version = release(36) }

    defaultConfig {
        applicationId = "com.lu4p.fokuslauncher"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "com.lu4p.fokuslauncher.HiltTestRunner"
    }

    if (hasReleaseSigning) {
        signingConfigs {
            create("release") {
                storeFile = file(releaseKeystorePath!!)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
            )
            isDebuggable = false
            isJniDebuggable = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        getByName("debug") {
            applicationIdSuffix = ".debug"
        }
    }

    buildFeatures { compose = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.google.material)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.activity.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Unit Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)

    // Android Testing
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
}
