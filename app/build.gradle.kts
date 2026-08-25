plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.krscripts.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.dna.tools"
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

// 去掉 Baseline Profile：排除 androidx.profileinstaller 运行时库
configurations.all {
    exclude(group = "androidx.profileinstaller", module = "profileinstaller")
}

// 停用 Baseline Profile / Startup Profile 生成任务，避免 assets/dexopt 被打进 APK
tasks.configureEach {
    val n = name.lowercase()
    if (n.contains("artprofile") || n.contains("startupprofile")) {
        enabled = false
    }
}
