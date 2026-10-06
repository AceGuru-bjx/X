plugins {
    id("unknown-android-application")
}

android {
    namespace = "com.unknown.security"

    defaultConfig {
        applicationId = "com.unknown.security"
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:persistence"))
    implementation(project(":core:designsystem"))
    implementation(project(":domain:engine"))
    implementation(project(":service:platform"))
    implementation(project(":service:accessibility"))
    implementation(project(":service:shizuku"))
    implementation(project(":service:root"))
    implementation(project(":service:deviceadmin"))
    implementation(project(":data:repository"))
    implementation(project(":feature:home"))
    implementation(project(":feature:scan"))
    implementation(project(":feature:shield"))
    implementation(project(":feature:rules"))
    implementation(project(":feature:settings"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
}
