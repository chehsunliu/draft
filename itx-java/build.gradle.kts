import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    alias(libs.plugins.spotless)
}

val sharedLibs = extensions.getByType<VersionCatalogsExtension>().named("libs")

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

    val lombok = sharedLibs.findLibrary("lombok").get()
    dependencies {
        "compileOnly"(lombok)
        "annotationProcessor"(lombok)
        "testCompileOnly"(lombok)
        "testAnnotationProcessor"(lombok)
    }

    extensions.configure<SpotlessExtension> {
        java {
            target("src/**/*.java")
            googleJavaFormat("1.30.0")
        }
    }
}
