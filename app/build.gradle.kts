import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

val configuredBackendBaseUrl = providers.gradleProperty("backendBaseUrl")
    .orElse(providers.environmentVariable("BACKEND_BASE_URL"))
    .orElse("https://api.example.com/")
    .get()

// Debug backend: emulator host by default; override with -PdebugBackendBaseUrl=http://<LAN-IP>:8080/
// or DEBUG_BACKEND_BASE_URL (debug builds only, never shipped).
val debugBackendBaseUrl = providers.gradleProperty("debugBackendBaseUrl")
    .orElse(providers.environmentVariable("DEBUG_BACKEND_BASE_URL"))
    .orElse("http://10.79.1.17:8080/")
    .get()
    .trim().trimEnd('/') + "/"

// Release signing: keystore.properties (git-ignored) or environment variables (CI).
// Keys: storeFile, storePassword, keyAlias, keyPassword.
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun signingValue(key: String, env: String): String? =
    keystoreProps.getProperty(key) ?: System.getenv(env)

fun requireProductionBackendUrl(url: String): String {
    val normalized = url.trim().trimEnd('/') + "/"
    require(normalized != "https://api.example.com/") {
        "Release builds require a real HTTPS backend URL. Set -PbackendBaseUrl or BACKEND_BASE_URL."
    }
    require(normalized.startsWith("https://")) {
        "Release backend URL must use HTTPS."
    }
    return normalized
}

android {
    namespace = "ir.hamedan.budgetmanagement"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "ir.hamedan.budgetmanagement"
        minSdk = 26
        targetSdk = 36
        // Play rejects an upload whose versionCode was already used: bump with -PversionCode=N.
        versionCode = providers.gradleProperty("versionCode").map { it.toInt() }.getOrElse(1)
        versionName = providers.gradleProperty("versionName").getOrElse("1.1.2")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "APP_NAME", "\"Cidna\"")
        buildConfigField("String", "BASE_URL", "\"https://api.example.com/\"")
    }

    flavorDimensions += "store"
    productFlavors {
        // Google Play: no SMS permissions (restricted by Play policy).
        create("play") {
            dimension = "store"
            buildConfigField("Boolean", "SMS_IMPORT_ENABLED", "false")
            manifestPlaceholders["smsReceiverEnabled"] = "false"
        }
        // Bazaar / Myket / direct APK: SMS-based transaction detection enabled.
        create("direct") {
            dimension = "store"
            buildConfigField("Boolean", "SMS_IMPORT_ENABLED", "true")
            manifestPlaceholders["smsReceiverEnabled"] = "true"
        }
    }

    signingConfigs {
        val storePath = signingValue("storeFile", "RELEASE_STORE_FILE")
        if (storePath != null) {
            create("release") {
                storeFile = rootProject.file(storePath)
                storePassword = signingValue("storePassword", "RELEASE_STORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "RELEASE_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            buildConfigField("String", "BASE_URL", "\"$debugBackendBaseUrl\"")
            buildConfigField("Boolean", "ENABLE_LOGS", "true")
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            buildConfigField(
                "String",
                "BASE_URL",
                "\"${configuredBackendBaseUrl.trim().trimEnd('/') + "/"}\""
            )
            buildConfigField("Boolean", "ENABLE_LOGS", "false")
            signingConfigs.findByName("release")?.let { signingConfig = it }
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

    // Make merged Android resources (including R.raw.export_logo_mark) available to Robolectric tests.
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE.md",
                "META-INF/NOTICE.txt",
                "META-INF/DEPENDENCIES",
                "META-INF/AL2.0",
                "META-INF/LGPL2.1"
            )
        }
    }
}

// Do not validate the production backend during Gradle configuration/sync.
// Android Studio evaluates the release build type while importing the project,
// even when you are only running the Debug variant on a physical device.
// Validate HTTPS only when a Release-related task is actually requested.
gradle.taskGraph.whenReady {
    val releaseRequested = allTasks.any { task ->
        task.name.contains("Release", ignoreCase = true)
    }

    if (releaseRequested) {
        requireProductionBackendUrl(configuredBackendBaseUrl)
        val publishing = allTasks.any { it.name.startsWith("bundle") && it.name.contains("Release") }
        require(!publishing || signingValue("storeFile", "RELEASE_STORE_FILE") != null) {
            "Release bundles must be signed: create keystore.properties (storeFile, storePassword, keyAlias, keyPassword) or set RELEASE_* environment variables."
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.core.ktx)
    implementation(libs.lottie.compose)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.security.crypto)
    implementation(libs.fastexcel)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.work.runtime.ktx)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.sqlcipher)

    // Test
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(libs.truth)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.work.testing)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.mockk.android)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}