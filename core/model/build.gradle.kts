plugins {
    id("unknown-kotlin-jvm")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(project(":core:common"))
}
