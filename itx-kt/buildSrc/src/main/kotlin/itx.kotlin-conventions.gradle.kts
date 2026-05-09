import com.diffplug.gradle.spotless.SpotlessExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("com.diffplug.spotless")
    id("org.jetbrains.kotlin.jvm")
}

repositories {
    mavenCentral()
}

// Kotlin 2.2.x maxes out at JVM target 24; the underlying toolchain stays on JDK 25 so we
// run on the host JDK, but the bytecode target is pinned to 24 to keep the Kotlin and
// Java compile targets in lockstep.
extensions.configure<KotlinJvmProjectExtension> {
    jvmToolchain(25)
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_24) }
}

extensions.configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_24
    targetCompatibility = JavaVersion.VERSION_24
}

extensions.configure<SpotlessExtension> {
    kotlin {
        target("src/**/*.kt")
        ktlint("1.5.0")
    }
}
