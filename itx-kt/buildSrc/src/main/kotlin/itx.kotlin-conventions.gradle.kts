import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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
kotlin {
    jvmToolchain(25)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_24)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_24
    targetCompatibility = JavaVersion.VERSION_24
}

spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint("1.5.0")
    }
}
