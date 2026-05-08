import com.diffplug.gradle.spotless.SpotlessExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.shadow) apply false
    alias(libs.plugins.spotless)
}

allprojects {
    group = "io.github.chehsunliu.itx"
    version = "0.1.0"
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "com.diffplug.spotless")

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

    repositories {
        mavenCentral()
    }

    extensions.configure<SpotlessExtension> {
        kotlin {
            target("src/**/*.kt")
            ktlint("1.5.0")
        }
    }
}
