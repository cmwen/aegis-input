plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

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
        versionCode = 2
        versionName = "0.1.1"

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
}

dependencies {
    implementation(project(":engine-rime"))
    implementation(project(":ui-compose"))

    implementation(platform(libs.compose.bom))
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
