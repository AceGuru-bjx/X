plugins {
    id("unknown-android-library")
    id("unknown-android-compose")
}

android {
    namespace = "com.unknown.security.feature.settings"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:designsystem"))
    implementation(project(":data:repository"))
    implementation(project(":service:accessibility"))
    implementation(project(":service:shizuku"))
    implementation(project(":service:root"))
    implementation(project(":service:deviceadmin"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
}
