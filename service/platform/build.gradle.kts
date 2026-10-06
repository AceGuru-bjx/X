plugins {
    id("unknown-android-library")
}

android {
    namespace = "com.unknown.security.service.platform"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
}
