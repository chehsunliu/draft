import com.diffplug.gradle.spotless.SpotlessExtension

plugins {
    alias(libs.plugins.spotless)
}

allprojects {
    group = "io.github.chehsunliu.itx"
    version = "0.1.0"
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "com.diffplug.spotless")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
    }

    repositories {
        mavenCentral()
    }

    extensions.configure<SpotlessExtension> {
        java {
            target("src/**/*.java")
            googleJavaFormat("1.30.0")
        }
    }
}
