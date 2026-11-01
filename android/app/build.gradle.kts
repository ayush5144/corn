plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "app.cornbrowser"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.cornbrowser"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-spike"
        ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
tasks.named("preBuild") { dependsOn(buildRustCore) }

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.webkit)
    implementation("${libs.jna.get().module}:${libs.jna.get().version}@aar")
    debugImplementation(libs.androidx.compose.ui.tooling)
}
