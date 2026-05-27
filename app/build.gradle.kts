import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

layout.buildDirectory.set(file("build"))
android {
    namespace = "flare.client.app"
    compileSdk = 36

    val localProperties = Properties().apply {
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { load(it) }
        }
    }
    val freeServersUrl = localProperties.getProperty("FREE_SERVERS_URL") ?: "https://example.com/placeholder"

    defaultConfig {
        applicationId = "flare.client.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 15
        versionName = "1.2.0"
        renderscriptTargetApi = 31
        renderscriptSupportModeEnabled = true

        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a"))
        }

        buildConfigField("String", "FREE_SERVERS_URL", "\"$freeServersUrl\"")
    }

    signingConfigs {
        create("release") {
            storeFile = file("release.key")
            storePassword = project.findProperty("RELEASE_STORE_PASSWORD")?.toString() ?: "YOUR_STORE_PASSWORD"
            keyAlias = project.findProperty("RELEASE_KEY_ALIAS")?.toString() ?: "release"
            keyPassword = project.findProperty("RELEASE_KEY_PASSWORD")?.toString() ?: "YOUR_KEY_PASSWORD"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }

        getByName("debug") {
            applicationIdSuffix = ".test"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += "/META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
    }
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.coroutines.android)
    implementation(libs.okhttp)
    implementation(libs.gson)
    implementation(libs.activity.ktx)
    implementation(libs.fragment.ktx)
    implementation(libs.sshj)
    implementation(libs.bouncycastle.prov)
    implementation(libs.bouncycastle.kix)
    implementation(libs.mlkit.barcode.scanning)
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(libs.zxing.core)
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.runtime)
    implementation(libs.activity.compose)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.haze)
    implementation(libs.navigation.compose)
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))
}
