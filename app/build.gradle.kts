plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.flashpro.qkptwd"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    
    ndk {
      abiFilters.addAll(setOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
    }
  }

  signingConfigs {
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("debugConfig")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
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
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}

val rootDirectoryPath = project.rootDir.absolutePath
val buildDirectoryPath = project.layout.buildDirectory.get().asFile.absolutePath

tasks.register("printApkSizes") {
    val debugOutputsDirPath = "${rootDirectoryPath}/.build-outputs"
    val debugApkPath = "${buildDirectoryPath}/outputs/apk/debug/app-debug.apk"
    
    doLast {
        fun printSize(filePath: String) {
            val f = File(filePath)
            if (f.exists()) {
                println("FILE: ${f.absolutePath} -- SIZE: ${f.length()} bytes")
            } else {
                println("FILE: ${f.absolutePath} does NOT exist.")
            }
        }
        println("=== APK SIZE REPORT ===")
        printSize("${debugOutputsDirPath}/app-debug.apk")
        printSize("${debugOutputsDirPath}/app-release.apk")
        printSize(debugApkPath)
    }
}

tasks.register("copyDebugToOutputs") {
    dependsOn("assembleDebug")
    val buildOutputsDirPath = "${rootDirectoryPath}/.build-outputs"
    val visibleOutputsDirPath = "${rootDirectoryPath}/outputs"
    val debugApkPath = "${buildDirectoryPath}/outputs/apk/debug/app-debug.apk"
    
    doLast {
        val debugApk = File(debugApkPath)
        val buildOutputsDir = File(buildOutputsDirPath)
        val visibleOutputsDir = File(visibleOutputsDirPath)
        
        if (debugApk.exists()) {
            // 1. Copy to .build-outputs for the platform's 'Download App' features
            if (!buildOutputsDir.exists()) {
                buildOutputsDir.mkdirs()
            }
            val targetDebug = File(buildOutputsDir, "app-debug.apk")
            debugApk.copyTo(targetDebug, overwrite = true)
            println("Copied debug APK to hidden outputs: ${targetDebug.absolutePath}")
            
            val targetRelease = File(buildOutputsDir, "app-release.apk")
            if (!targetRelease.exists()) {
                debugApk.copyTo(targetRelease, overwrite = true)
                println("Copied debug APK as release placeholder to: ${targetRelease.absolutePath}")
            } else {
                println("A release APK already exists; skipping placeholder copy.")
            }
            
            // 2. Copy to a visible 'outputs' directory for sidebar visibility and ZIP download exports
            if (!visibleOutputsDir.exists()) {
                visibleOutputsDir.mkdirs()
            }
            val targetVisibleDebug = File(visibleOutputsDir, "app-debug.apk")
            debugApk.copyTo(targetVisibleDebug, overwrite = true)
            println("Copied debug APK to visible outputs: ${targetVisibleDebug.absolutePath}")
        } else {
            throw GradleException("ERROR: Debug APK not found at ${debugApk.absolutePath}. Please run assembleDebug first.")
        }
    }
}

tasks.register("copyReleaseToOutputs") {
    dependsOn("assembleRelease")
    val buildOutputsDirPath = "${rootDirectoryPath}/.build-outputs"
    val visibleOutputsDirPath = "${rootDirectoryPath}/outputs"
    val releaseApkPath = "${buildDirectoryPath}/outputs/apk/release/app-release.apk"
    
    doLast {
        val releaseApk = File(releaseApkPath)
        val buildOutputsDir = File(buildOutputsDirPath)
        val visibleOutputsDir = File(visibleOutputsDirPath)
        
        if (releaseApk.exists()) {
            if (!buildOutputsDir.exists()) {
                buildOutputsDir.mkdirs()
            }
            val targetRelease = File(buildOutputsDir, "app-release.apk")
            releaseApk.copyTo(targetRelease, overwrite = true)
            println("Copied real release APK to hidden outputs: ${targetRelease.absolutePath}")
            
            if (!visibleOutputsDir.exists()) {
                visibleOutputsDir.mkdirs()
            }
            val targetVisibleRelease = File(visibleOutputsDir, "app-release.apk")
            releaseApk.copyTo(targetVisibleRelease, overwrite = true)
            println("Copied real release APK to visible outputs: ${targetVisibleRelease.absolutePath}")
        } else {
            throw GradleException("ERROR: Release APK not found at ${releaseApk.absolutePath}. Please run assembleRelease first.")
        }
    }
}

afterEvaluate {
    tasks.named("assembleDebug") {
        finalizedBy("copyDebugToOutputs")
    }
    tasks.named("assembleRelease") {
        finalizedBy("copyReleaseToOutputs")
    }
}

