plugins {
    id("unknown-android-library")
}

android {
    namespace = "com.unknown.security.service.shizuku"

    buildFeatures {
        aidl = true
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
}
