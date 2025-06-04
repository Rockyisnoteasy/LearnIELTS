plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp") version "1.9.10-1.0.13"
}

android {
    namespace = "com.example.learnielts"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.learnielts"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        multiDexEnabled = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/INDEX.LIST",
                "META-INF/io.netty.versions.properties",
                "META-INF/DEPENDENCIES",
                "META-INF/NOTICE",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE.txt"
            )
        }
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
}

dependencies {
    // Jetpack Compose
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended:1.6.0")
    implementation("androidx.activity:activity-compose:1.8.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation("androidx.compose.ui:ui:1.5.4")
    implementation("androidx.compose.material3:material3:1.1.2")

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // CSV
    implementation("com.opencsv:opencsv:5.7.1")

    // OkHttp（for Tencent TTS）
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:okhttp-urlconnection:4.12.0")

    // ✅ Google Cloud Text-to-Speech
    implementation("com.google.cloud:google-cloud-texttospeech:2.34.0")
    implementation("com.google.protobuf:protobuf-java:3.21.12")
    implementation("com.google.api.grpc:proto-google-common-protos:2.14.0")

    implementation("io.grpc:grpc-okhttp:1.53.0")
    implementation("io.grpc:grpc-core:1.53.0")
    implementation("io.grpc:grpc-stub:1.53.0")
    implementation("io.grpc:grpc-protobuf:1.53.0") // ✅ 完整版 protobuf 支持

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.auth:google-auth-library-oauth2-http:1.18.0")

    implementation("com.wajahatkarim:flippable:1.5.4")

    implementation("org.apache.poi:poi-ooxml:5.2.3")
    implementation("androidx.multidex:multidex:2.0.1")


    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")


    configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "io.grpc" && requested.name == "grpc-netty") {
            useTarget("io.grpc:grpc-okhttp:1.53.0")
        }
        if (requested.group == "io.grpc" && requested.name == "grpc-netty-shaded") {
            useTarget("io.grpc:grpc-okhttp:1.53.0")
        }
    }

    exclude(group = "io.grpc", module = "grpc-netty")
    exclude(group = "io.grpc", module = "grpc-netty-shaded")
}

    // 测试相关
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
