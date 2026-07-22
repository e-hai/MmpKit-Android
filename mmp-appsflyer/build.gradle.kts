plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.kit.mmp.appsflyer"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    api(project(":mmp-core"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.appsflyer.sdk)
    implementation(libs.installreferrer)
}
