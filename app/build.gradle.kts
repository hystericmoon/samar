import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.oss.licenses)
}

android {
    namespace = "app.samar"
    compileSdk = 37

    defaultConfig {
        applicationId = "app.samar"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    // The upload key lives outside the project (~/samar-keys), never in this folder.
    // Without it, release builds come out unsigned.
    val uploadKey = File(System.getProperty("user.home"), "samar-keys/upload.properties")
        .takeIf { it.isFile }
        ?.let { f -> Properties().apply { f.inputStream().use(::load) } }

    signingConfigs {
        if (uploadKey != null) {
            create("upload") {
                storeFile = file(uploadKey.getProperty("storeFile"))
                storePassword = uploadKey.getProperty("storePassword")
                keyAlias = uploadKey.getProperty("keyAlias")
                keyPassword = uploadKey.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (uploadKey != null) signingConfig = signingConfigs.getByName("upload")
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.exifinterface)
    // Bundled model: works offline, no download from Google Play services.
    implementation(libs.mlkit.face.detection)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
