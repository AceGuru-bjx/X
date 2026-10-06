import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

android {
    buildFeatures {
        compose = true
    }
}

dependencies {
    add("implementation", platform(libs.findLibrary("androidx-compose-bom").get()))
    add("implementation", libs.findLibrary("androidx-compose-foundation").get())
    add("implementation", libs.findLibrary("androidx-compose-ui").get())
    add("implementation", libs.findLibrary("androidx-compose-ui-graphics").get())
    add("implementation", libs.findLibrary("androidx-compose-material3").get())
    add("implementation", libs.findLibrary("androidx-compose-material-icons").get())
    add("implementation", libs.findLibrary("androidx-compose-animation").get())
}
