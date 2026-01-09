import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Read API keys from local.properties
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(FileInputStream(localPropertiesFile))
            val geminiApiKey = localProperties.getProperty("GEMINI_API_KEY", "")
            val huggingFaceToken = localProperties.getProperty("HUGGINGFACE_API_TOKEN", "")
            val openAiApiKey = localProperties.getProperty("OPENAI_API_KEY", "")
            // Ensure values are properly quoted as strings
            val geminiKeyValue = if (geminiApiKey.isNotEmpty()) "\"$geminiApiKey\"" else "\"\""
            val huggingFaceTokenValue = if (huggingFaceToken.isNotEmpty()) "\"$huggingFaceToken\"" else "\"\""
            val openAiKeyValue = if (openAiApiKey.isNotEmpty()) "\"$openAiApiKey\"" else "\"\""
            buildConfigField("String", "GEMINI_API_KEY", geminiKeyValue)
            buildConfigField("String", "HUGGINGFACE_API_TOKEN", huggingFaceTokenValue)
            buildConfigField("String", "OPENAI_API_KEY", openAiKeyValue)
        } else {
            buildConfigField("String", "GEMINI_API_KEY", "\"\"")
            buildConfigField("String", "HUGGINGFACE_API_TOKEN", "\"\"")
            buildConfigField("String", "OPENAI_API_KEY", "\"\"")
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
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.localbroadcastmanager)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.firebase.database)
    implementation(libs.firebase.auth)
    implementation(libs.zxing.core)
    implementation(libs.zxing.android)
    // Gemini AI dependency for AI chatbot feature
    implementation("com.google.ai.client.generativeai:generativeai:0.7.0")
    // HTTP client for Hugging Face API
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation(libs.play.services.ads)
    implementation(libs.guava)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}