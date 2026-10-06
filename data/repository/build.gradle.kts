plugins {
    id("unknown-android-library")
}

android {
    namespace = "com.unknown.security.data.repository"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:persistence"))
    implementation(project(":domain:engine"))
    implementation(project(":service:platform"))
    implementation(project(":service:accessibility"))
    implementation(project(":service:shizuku"))
    implementation(project(":service:root"))
    implementation(project(":service:deviceadmin"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
}
