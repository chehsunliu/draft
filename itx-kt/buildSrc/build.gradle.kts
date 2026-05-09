plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation(libs.gradle.plugin.kotlin.jvm)
    implementation(libs.gradle.plugin.kotlin.serialization)
    implementation(libs.gradle.plugin.shadow)
    implementation(libs.gradle.plugin.spotless)
}
