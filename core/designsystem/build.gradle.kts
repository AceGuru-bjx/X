plugins {
    id("unknown-android-library")
    id("unknown-android-compose")
}

android {
    namespace = "com.unknown.security.core.designsystem"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    api(libs.kyant.backdrop)
    api(libs.kyant.shapes)
    implementation(libs.androidx.core.ktx)
}
