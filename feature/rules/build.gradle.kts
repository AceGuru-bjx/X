plugins {
    id("unknown-android-library")
    id("unknown-android-compose")
}

android {
    namespace = "com.unknown.security.feature.rules"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:persistence"))
    implementation(project(":core:designsystem"))
    implementation(project(":data:repository"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
}
