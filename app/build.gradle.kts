import java.util.Properties

val localProperties = Properties().apply {
    load(rootProject.file("local.properties").inputStream())
}

val cloudinaryCloudName = localProperties["cloudinary_name"] as String
val cloudinaryApiKey = localProperties["cloudinary_api_key"] as String
val cloudinaryApiSecret = localProperties["cloudinary_api_secret"] as String
val mailUsername = localProperties["mail_username"] as String
val mailPassword = localProperties["mail_password"] as String

val keyStorePassword = localProperties["key_store_password"] as String

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    id("com.google.dagger.hilt.android") version "2.56.1"
    id("kotlin-kapt")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.android.dacs3"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.android.dacs3"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "CloudinaryCloudName", "\"$cloudinaryCloudName\"")
        buildConfigField("String", "CloudinaryApiKey", "\"$cloudinaryApiKey\"")
        buildConfigField("String", "CloudinaryApiSecret", "\"$cloudinaryApiSecret\"")
        buildConfigField("String", "MailUsername", "\"$mailUsername\"")
        buildConfigField("String", "MailPassword", "\"$mailPassword\"")
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

    packaging {
        resources {
            excludes += setOf(
                "META-INF/LICENSE.md",
                "META-INF/LICENSE",
                "META-INF/NOTICE.md"
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
        buildConfig = true
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.play.services.cast.framework)
    implementation(libs.androidx.runtime.livedata)
    implementation(libs.androidx.scenecore)
    implementation(files("D:\\Downloads\\zpdk-release-v3.1.aar"))
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // Hilt (đồng bộ version 2.56.1)
    implementation("com.google.dagger:hilt-android:2.56.1")
    kapt("com.google.dagger:hilt-android-compiler:2.56.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    kapt("androidx.hilt:hilt-compiler:1.0.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.12.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation ("com.google.android.gms:play-services-auth:20.7.0")

    // Fragment
    implementation("androidx.fragment:fragment-ktx:1.4.0")

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("io.coil-kt:coil-compose:2.4.0")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Nếu dùng coroutine:
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // accompanist dung de load image
    implementation("com.google.accompanist:accompanist-swiperefresh:0.33.0-alpha")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.24.0-alpha")
    implementation("com.google.accompanist:accompanist-flowlayout:0.30.1")

    implementation ("com.google.android.gms:play-services-base:18.2.0")

    // Cloudinary
    implementation ("com.cloudinary:cloudinary-android:2.3.1")

    // Android Mail
    implementation("com.sun.mail:android-mail:1.6.7")
    implementation("com.sun.mail:android-activation:1.6.7")


}
