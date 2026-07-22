import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

fun local(key: String, default: String = ""): String =
    localProperties.getProperty(key, default).orEmpty()

fun localBool(key: String, default: Boolean): Boolean =
    localProperties.getProperty(key)?.toBooleanStrictOrNull() ?: default

fun escapeForBuildConfig(value: String): String =
    value.replace("\\", "\\\\").replace("\"", "\\\"")

android {
    namespace = "com.kit.mmp.sample"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.kit.mmp.sample"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Read MMP demo credentials / switches from root local.properties
        buildConfigField(
            "String",
            "MMP_PLATFORM",
            "\"${escapeForBuildConfig(local("mmp.platform", "appsflyer"))}\"",
        )
        buildConfigField(
            "String",
            "MMP_APP_TOKEN",
            "\"${escapeForBuildConfig(local("mmp.appToken", "SAMPLE_DEV_KEY"))}\"",
        )
        buildConfigField(
            "String",
            "MMP_APP_ID",
            "\"${escapeForBuildConfig(local("mmp.appId"))}\"",
        )
        buildConfigField("boolean", "MMP_IS_DEBUG", "${localBool("mmp.isDebug", true)}")
        buildConfigField("boolean", "MMP_ENABLE_LOG", "${localBool("mmp.enableLog", true)}")
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
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

dependencies {
    implementation(project(":mmp-core"))
    implementation(project(":mmp-appsflyer"))
    implementation(project(":mmp-adjust"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
