import java.util.Properties
import org.gradle.api.GradleException

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.compose.compiler)
}

// Gradle only reads gradle.properties from the repo root and ~/.gradle — not from project/.gradle/.
// Optional gitignored file at repo root: signing.properties (same keys as env / gradle.properties).
val signingProperties = Properties()
val signingPropertiesFile = rootProject.file("signing.properties")
if (signingPropertiesFile.exists()) {
    signingPropertiesFile.inputStream().use { signingProperties.load(it) }
}

fun releaseSigningValue(vararg names: String): String? {
    for (name in names) {
        System.getenv(name)?.takeIf { it.isNotBlank() }?.let { return it }
        signingProperties.getProperty(name)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        (project.findProperty(name) as String?)?.takeIf { it.isNotBlank() }?.let { return it }
    }
    return null
}

val requestedTaskNames = gradle.startParameter.taskNames.map { it.lowercase() }
val requiresReleaseBundleSigning = requestedTaskNames.any { it.contains("bundlerelease") }
val requiresReleaseApkSigning = requestedTaskNames.any { it.contains("assemblerelease") }

val apkKeystorePath = releaseSigningValue("ANDROID_APK_KEYSTORE_PATH", "ANDROID_KEYSTORE_PATH")
val apkKeystorePassword = releaseSigningValue("ANDROID_APK_KEYSTORE_PASSWORD", "ANDROID_KEYSTORE_PASSWORD")
val apkKeyAlias = releaseSigningValue("ANDROID_APK_KEY_ALIAS", "ANDROID_KEY_ALIAS")
val apkKeyPassword = releaseSigningValue("ANDROID_APK_KEY_PASSWORD", "ANDROID_KEY_PASSWORD")
val hasApkReleaseSigning = !apkKeystorePath.isNullOrBlank() &&
        !apkKeystorePassword.isNullOrBlank() &&
        !apkKeyAlias.isNullOrBlank() &&
        !apkKeyPassword.isNullOrBlank()
val missingApkReleaseSigningInputs = buildList {
    if (apkKeystorePath.isNullOrBlank()) add("ANDROID_APK_KEYSTORE_PATH/ANDROID_KEYSTORE_PATH")
    if (apkKeystorePassword.isNullOrBlank()) add("ANDROID_APK_KEYSTORE_PASSWORD/ANDROID_KEYSTORE_PASSWORD")
    if (apkKeyAlias.isNullOrBlank()) add("ANDROID_APK_KEY_ALIAS/ANDROID_KEY_ALIAS")
    if (apkKeyPassword.isNullOrBlank()) add("ANDROID_APK_KEY_PASSWORD/ANDROID_KEY_PASSWORD")
}

val bundleKeystorePath = releaseSigningValue("ANDROID_BUNDLE_KEYSTORE_PATH", "ANDROID_KEYSTORE_PATH")
val bundleKeystorePassword = releaseSigningValue("ANDROID_BUNDLE_KEYSTORE_PASSWORD", "ANDROID_KEYSTORE_PASSWORD")
val bundleKeyAlias = releaseSigningValue("ANDROID_BUNDLE_KEY_ALIAS", "ANDROID_KEY_ALIAS")
val bundleKeyPassword = releaseSigningValue("ANDROID_BUNDLE_KEY_PASSWORD", "ANDROID_KEY_PASSWORD")
val hasBundleReleaseSigning = !bundleKeystorePath.isNullOrBlank() &&
        !bundleKeystorePassword.isNullOrBlank() &&
        !bundleKeyAlias.isNullOrBlank() &&
        !bundleKeyPassword.isNullOrBlank()
val missingBundleReleaseSigningInputs = buildList {
    if (bundleKeystorePath.isNullOrBlank()) add("ANDROID_BUNDLE_KEYSTORE_PATH/ANDROID_KEYSTORE_PATH")
    if (bundleKeystorePassword.isNullOrBlank()) add("ANDROID_BUNDLE_KEYSTORE_PASSWORD/ANDROID_KEYSTORE_PASSWORD")
    if (bundleKeyAlias.isNullOrBlank()) add("ANDROID_BUNDLE_KEY_ALIAS/ANDROID_KEY_ALIAS")
    if (bundleKeyPassword.isNullOrBlank()) add("ANDROID_BUNDLE_KEY_PASSWORD/ANDROID_KEY_PASSWORD")
}

// Unsigned release builds (e.g. reproducible builds, F-Droid/IzzyOnDroid) are allowed when no keystore path is set.
val wantsApkReleaseSigning = !apkKeystorePath.isNullOrBlank()
val wantsBundleReleaseSigning = !bundleKeystorePath.isNullOrBlank()

if (requiresReleaseApkSigning && wantsApkReleaseSigning && missingApkReleaseSigningInputs.isNotEmpty()) {
    throw GradleException(
            "APK release signing was requested (keystore path set) but configuration is incomplete. " +
                    "Set ANDROID_APK_KEYSTORE_PATH, ANDROID_APK_KEYSTORE_PASSWORD, ANDROID_APK_KEY_ALIAS, and " +
                    "ANDROID_APK_KEY_PASSWORD (or the generic ANDROID_KEYSTORE_PATH, ANDROID_KEYSTORE_PASSWORD, " +
                    "ANDROID_KEY_ALIAS, ANDROID_KEY_PASSWORD). " +
                    "Missing: ${missingApkReleaseSigningInputs.joinToString(", ")}"
    )
}

if (requiresReleaseBundleSigning && wantsBundleReleaseSigning && missingBundleReleaseSigningInputs.isNotEmpty()) {
    throw GradleException(
            "Bundle release signing was requested (keystore path set) but configuration is incomplete. " +
                    "Set ANDROID_BUNDLE_KEYSTORE_PATH, ANDROID_BUNDLE_KEYSTORE_PASSWORD, ANDROID_BUNDLE_KEY_ALIAS, and " +
                    "ANDROID_BUNDLE_KEY_PASSWORD (or the generic ANDROID_KEYSTORE_PATH, ANDROID_KEYSTORE_PASSWORD, " +
                    "ANDROID_KEY_ALIAS, ANDROID_KEY_PASSWORD). " +
                    "Missing: ${missingBundleReleaseSigningInputs.joinToString(", ")}"
    )
}

val apkAndBundleUseDifferentSigners = hasApkReleaseSigning &&
        hasBundleReleaseSigning &&
        (
                apkKeystorePath != bundleKeystorePath ||
                        apkKeystorePassword != bundleKeystorePassword ||
                        apkKeyAlias != bundleKeyAlias ||
                        apkKeyPassword != bundleKeyPassword
                )

if (requiresReleaseApkSigning && requiresReleaseBundleSigning && apkAndBundleUseDifferentSigners) {
    throw GradleException(
            "assembleRelease and bundleRelease are configured to use different signing keys. " +
                    "Run them in separate Gradle invocations."
    )
}

android {
    namespace = "com.lu4p.fokuslauncher"
    compileSdk { version = release(36) }

    // Disable dependency metadata in APK signing block (required for F-Droid)
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    defaultConfig {
        applicationId = "io.github.luantak.fokuslauncher"
        minSdk = 26
        targetSdk = 36
        versionCode = 36
        versionName = "1.5.0"

        testInstrumentationRunner = "io.github.luantak.fokuslauncher.HiltTestRunner"
    }

    if (hasApkReleaseSigning || hasBundleReleaseSigning) {
        signingConfigs {
            if (hasApkReleaseSigning) {
                create("releaseApk") {
                    // Resolve against repo root so paths like ".keystore/release.keystore" work (app/file() would use app/).
                    storeFile = rootProject.file(apkKeystorePath!!)
                    storePassword = apkKeystorePassword
                    keyAlias = apkKeyAlias
                    keyPassword = apkKeyPassword
                }
            }
            if (hasBundleReleaseSigning) {
                create("releaseBundle") {
                    storeFile = rootProject.file(bundleKeystorePath!!)
                    storePassword = bundleKeystorePassword
                    keyAlias = bundleKeyAlias
                    keyPassword = bundleKeyPassword
                }
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
            // Emit Play Console native debug metadata for dependency-provided .so files.
            ndk {
                debugSymbolLevel = "FULL"
            }
            when {
                requiresReleaseBundleSigning && hasBundleReleaseSigning -> {
                    signingConfig = signingConfigs.getByName("releaseBundle")
                }
                requiresReleaseApkSigning && hasApkReleaseSigning -> {
                    signingConfig = signingConfigs.getByName("releaseApk")
                }
                hasBundleReleaseSigning -> {
                    signingConfig = signingConfigs.getByName("releaseBundle")
                }
                hasApkReleaseSigning -> {
                    signingConfig = signingConfigs.getByName("releaseApk")
                }
                else -> {
                    signingConfig = null
                }
            }
        }
        getByName("debug") {
            applicationIdSuffix = ".debug"
        }
    }

    buildFeatures { compose = true }

    // Keep symbols in this native lib to avoid environment-dependent stripping output.
    packaging {
        jniLibs {
            keepDebugSymbols += "**/libdatastore_shared_counter.so"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    bundle {
        language {
            enableSplit = false
        }
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
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
    testImplementation(libs.mockwebserver)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)

    // Android Testing
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.turbine)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
}
