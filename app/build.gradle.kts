plugins {
    alias(libs.plugins.android.application)
}

import java.util.Properties

// 从 local.properties（已 gitignore）读取 release 签名配置，避免密钥入库
// 无 local.properties（如 CI）时回退到仓库内的 AOSP testkey（公开密钥，仅用于对上系统 signature 权限）
val releaseProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val keystorePass = releaseProps.getProperty("RELEASE_STORE_PASSWORD", "android")
val releaseKeyAlias = releaseProps.getProperty("RELEASE_KEY_ALIAS", "android")
val keyPass = releaseProps.getProperty("RELEASE_KEY_PASSWORD", "android")

android {
    namespace = "com.krscripts.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.dna.tools"
        minSdk = 23
        targetSdk = 28
        versionCode = 20260825
        versionName = "260825"
        buildConfigField("String", "FRAMEWORK_VERSION", "\"0.2.0\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // 相对路径一律相对仓库根解析（CI 下无 local.properties 走默认），绝对路径原样使用
            storeFile = releaseProps.getProperty("RELEASE_STORE_FILE")?.let { file(it) }
                ?: rootProject.file("keystore/testkey/testkey.p12")
            storePassword = keystorePass
            keyAlias = releaseKeyAlias
            keyPassword = keyPass
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
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
