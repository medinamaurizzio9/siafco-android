import org.gradle.api.GradleException
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val releaseBaseUrlProvider = providers.gradleProperty("SIAFCO_API_BASE_URL")
    .orElse(providers.environmentVariable("SIAFCO_API_BASE_URL"))

gradle.taskGraph.whenReady {
    val buildsRelease = allTasks.any { task ->
        task.path.contains("Release", ignoreCase = true) ||
            task.name.contains("Release", ignoreCase = true)
    }
    if (buildsRelease) {
        val url = releaseBaseUrlProvider.orNull
        if (url.isNullOrBlank()) {
            throw GradleException("Release requiere SIAFCO_API_BASE_URL por propiedad Gradle o variable de entorno.")
        }
        if (!url.startsWith("https://")) {
            throw GradleException("Release requiere SIAFCO_API_BASE_URL con HTTPS.")
        }
    }
}

android {
    namespace = "bo.org.siafco.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "bo.org.siafco.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8000/api/mobile/v1/\"")
            buildConfigField("Boolean", "ENABLE_NETWORK_LOGGING", "true")
            manifestPlaceholders["usesCleartextTraffic"] = "true"
        }
        release {
            isMinifyEnabled = false
            buildConfigField("String", "BASE_URL", "\"${releaseBaseUrlProvider.orNull.orEmpty()}\"")
            buildConfigField("Boolean", "ENABLE_NETWORK_LOGGING", "false")
            manifestPlaceholders["usesCleartextTraffic"] = "false"
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)
    implementation(libs.coil.compose)

    debugImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.okhttp.logging)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockwebserver)
    testImplementation(platform(libs.okhttp.bom))
    testImplementation(libs.okhttp)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
