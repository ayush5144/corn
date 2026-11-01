plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "app.cornbrowser"
    compileSdk = 37

    defaultConfig {
        applicationId = "app.cornbrowser"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.2.0"
    }

    buildTypes {
        debug {
            ndk { abiFilters += listOf("arm64-v8a", "x86_64") } // phone + Mac emulator
        }
        release {
            ndk { abiFilters += listOf("arm64-v8a") } // phones only; the x86_64 library is ~4.4 MB of emulator-only code
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    sourceSets.named("main") {
        // Kotlin bindings generated from the Rust core by UniFFI (see scripts/build-android-core.sh).
        kotlin.directories += "build/generated/uniffi/kotlin"
        jniLibs.directories += "build/generated/uniffi/jniLibs"
        // Offline starter list + cookie-banner script (see scripts/build-starter-assets.sh).
        assets.directories += "build/generated/starter/assets"
    }
}

// Build the Rust core (Brave's adblock engine + Corn code) for Android and generate Kotlin bindings.
val buildRustCore by tasks.registering(Exec::class) {
    val repoRoot = layout.projectDirectory.dir("../..").asFile
    workingDir = repoRoot
    commandLine("bash", "scripts/build-android-core.sh")
    inputs.dir(File(repoRoot, "core/crates"))
    inputs.file(File(repoRoot, "core/Cargo.toml"))
    outputs.dir(layout.projectDirectory.dir("build/generated/uniffi"))
}
// Offline starter list (compiled filter lists) and the pinned cookie-banner script.
val buildStarterAssets by tasks.registering(Exec::class) {
    workingDir = layout.projectDirectory.dir("../..").asFile
    commandLine("bash", "scripts/build-starter-assets.sh")
    dependsOn(buildRustCore)
    outputs.upToDateWhen { false } // the script itself decides what's fresh
}
tasks.named("preBuild") { dependsOn(buildRustCore, buildStarterAssets) }

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.webkit)
    implementation(libs.androidx.swiperefresh)
    implementation("${libs.jna.get().module}:${libs.jna.get().version}@aar")
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
}
