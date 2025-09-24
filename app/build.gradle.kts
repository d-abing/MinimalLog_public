plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.aube.minimallog"
    compileSdk = 35

    packaging {
        resources {
            excludes += "/META-INF/DEPENDENCIES"
        }
    }

    defaultConfig {
        applicationId = "com.aube.minimallog"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
             manifestPlaceholders["ADMOB_APP_ID"] = "ca-app-pub-3940256099942544~3347511713"
             buildConfigField(
                            "String",
                            "ADMOB_BANNER_ID",
                            "\"ca-app-pub-3940256099942544/6300978111\""
                        )
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }

        release {
             manifestPlaceholders["ADMOB_APP_ID"] = project.findProperty("ADMOB_APP_ID")?.toString() ?: ""
             buildConfigField(
                            "String",
                            "ADMOB_BANNER_ID",
                            "\"${project.findProperty("ADMOB_BANNER_ID") ?: ""}\""
                        )
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk {
                debugSymbolLevel = "FULL"
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
        buildConfig = true
    }
}

ksp {
    arg("dagger.fastInit", "enabled")
    arg("dagger.experimentalDaggerErrorMessages", "enabled")
}

dependencies {
    // hilt
    implementation(libs.dagger.hilt.android)
    implementation(libs.androidx.appcompat)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    // Datastore
    implementation(libs.androidx.datastore.preferences)

    // Admob
     implementation(libs.play.services.ads)

    // StatusBar
    implementation(libs.accompanist.systemuicontroller)

    // Navigation
    implementation(libs.androidx.navigation.compose.android)

    // Coil
    implementation(libs.coil.compose)

    // Picture Crop
    implementation(libs.canhub.android.image.cropper)

    // Google Drive
    implementation(libs.play.services.auth)

    // Google API Client (Android 변형)
    implementation(libs.google.api.client.android)
    implementation(libs.google.http.client.android)

    // Drive v3 REST
    implementation(libs.google.api.services.drive)

    // Coroutine
    implementation(libs.jetbrains.kotlinx.coroutines.play.services)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}