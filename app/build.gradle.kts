plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.protobuf)
}

// Room schema export for migrations
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

android {
    namespace = "com.harrisonog.musicvisualizer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.harrisonog.musicvisualizer"
        minSdk = 26 // Updated from 24 to match implementation plan
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }

    sourceSets {
        getByName("main") {
            java.srcDir("build/generated/source/proto/main/java")
            kotlin.srcDir("build/generated/source/proto/main/kotlin")
        }
        getByName("debug") {
            java.srcDir("build/generated/source/proto/debug/java")
            kotlin.srcDir("build/generated/source/proto/debug/kotlin")
        }
        getByName("release") {
            java.srcDir("build/generated/source/proto/release/java")
            kotlin.srcDir("build/generated/source/proto/release/kotlin")
        }
    }
}

// Protobuf configuration for DataStore
protobuf {
    protoc {
        artifact = libs.protobuf.protoc.get().toString()
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") {
                    option("lite")
                }
                create("kotlin") {
                    option("lite")
                }
            }
        }
    }
}

// Ensure proto generation runs before KSP
androidComponents {
    onVariants(selector().all()) { variant ->
        afterEvaluate {
            val variantName = variant.name.replaceFirstChar { it.uppercase() }
            val kspTask = project.tasks.findByName("ksp${variantName}Kotlin")
            val protoTask = project.tasks.findByName("generate${variantName}Proto")
            if (kspTask != null && protoTask != null) {
                kspTask.dependsOn(protoTask)
            }
        }
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Compose
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Hilt - Dependency Injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // DataStore with Protobuf
    implementation(libs.datastore)
    implementation(libs.datastore.preferences)
    implementation(libs.protobuf.kotlin.lite)

    // Media3 (ExoPlayer)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.media3.ui)
    implementation(libs.media3.common)

    // FFT Library for audio visualization
    implementation(libs.jtransforms)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.guava)

    // Image Loading
    implementation(libs.coil.compose)

    // Accompanist for permissions
    implementation(libs.accompanist.permissions)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
