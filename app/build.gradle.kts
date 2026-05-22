plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

fun getVersionFromGit(): Pair<Int, String> {
    return try {
        val process = Runtime.getRuntime().exec(arrayOf("git", "describe", "--tags", "--always"))
        val version = process.inputStream.bufferedReader().readText().trim()
        process.waitFor()
        
        if (version.startsWith("v")) {
            val parts = version.substring(1).split(".")
            if (parts.size == 3) {
                val major = parts[0].toIntOrNull() ?: 0
                val minor = parts[1].toIntOrNull() ?: 0
                val patch = parts[2].toIntOrNull() ?: 0
                val versionCode = major * 10000 + minor * 100 + patch
                val versionName = "$major.$minor.$patch"
                versionCode to versionName
            } else {
                12 to "0.1.12"
            }
        } else {
            12 to "0.1.12"
        }
    } catch (e: Exception) {
        12 to "0.1.12"
    }
}

val (gitVersionCode, gitVersionName) = getVersionFromGit()

val releaseKeystorePath = providers.environmentVariable("AEGISINPUT_RELEASE_KEYSTORE_PATH")
val releaseKeyAlias = providers.environmentVariable("AEGISINPUT_RELEASE_KEY_ALIAS")
val releaseKeyPassword = providers.environmentVariable("AEGISINPUT_RELEASE_KEY_PASSWORD")
val releaseStorePassword = providers.environmentVariable("AEGISINPUT_RELEASE_STORE_PASSWORD")
val hasReleaseSigning = releaseKeystorePath.isPresent &&
    releaseKeyAlias.isPresent &&
    releaseKeyPassword.isPresent &&
    releaseStorePassword.isPresent

android {
    namespace = "com.aegisinput.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aegisinput.app"
        minSdk = 26
        targetSdk = 35
        val envVersionCode = providers.environmentVariable("APP_VERSION_CODE")
        val envVersionName = providers.environmentVariable("APP_VERSION_NAME")

        versionCode = if (envVersionCode.isPresent) (envVersionCode.get().toIntOrNull() ?: gitVersionCode) else gitVersionCode
        versionName = if (envVersionName.isPresent) envVersionName.get() else gitVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseKeystorePath.get())
                storePassword = releaseStorePassword.get()
                keyAlias = releaseKeyAlias.get()
                keyPassword = releaseKeyPassword.get()
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
            if (hasReleaseSigning) {
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
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }

    ndkVersion = "28.2.13676358"
}

dependencies {
    implementation(project(":engine-rime"))
    implementation(project(":ui-compose"))

    implementation(platform(libs.compose.bom))
    implementation(libs.activity.compose)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.runtime)

    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.coroutines.android)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.espresso.core)
}
