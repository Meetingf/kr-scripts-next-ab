plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.krscripts.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.krscripts.app"
        minSdk = 23
        targetSdk = 37
        versionCode = 2
        versionName = "0.2.0"
        buildConfigField("String", "FRAMEWORK_VERSION", "\"0.2.0\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.material)
    implementation(project(":core"))
}
