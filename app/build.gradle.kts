import com.android.build.api.dsl.ApplicationExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.kotlin.kapt")
}

// Work around occasional Windows file-locking of app/build intermediates.
layout.buildDirectory.set(rootProject.layout.buildDirectory.dir("app-module"))

val yandexMapsApiKey: String = run {
    val propsFile = rootProject.file("local.properties")
    if (!propsFile.exists()) {
        ""
    } else {
        propsFile.readLines()
            .firstOrNull { it.startsWith("yandex.maps.api.key=") }
            ?.substringAfter("=")
            ?.trim()
            .orEmpty()
    }
}

val backendBaseUrl: String = run {
    val propsFile = rootProject.file("local.properties")
    if (!propsFile.exists()) {
        "http://10.0.2.2:8080/"
    } else {
        propsFile.readLines()
            .firstOrNull { it.startsWith("backend.base.url=") }
            ?.substringAfter("=")
            ?.trim()
            .orEmpty()
            .ifBlank { "http://10.0.2.2:8080/" }
    }
}

extensions.configure<ApplicationExtension>("android") {
    namespace = "com.topit.ecotrace"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.topit.ecotrace"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "YANDEX_MAPS_API_KEY", "\"$yandexMapsApiKey\"")
        buildConfigField("String", "BACKEND_BASE_URL", "\"$backendBaseUrl\"")

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
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.dagger)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.yandex.maps.mobile)
    implementation(libs.play.services.location)
    implementation(libs.androidx.compose.ui.text.google.fonts)
    implementation(libs.coil.compose)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging.interceptor)
    kapt(libs.androidx.room.compiler)
    kapt(libs.dagger.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
