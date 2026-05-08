import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    java
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
    alias(libs.plugins.spotless)
}

allprojects {
    group = "io.github.chehsunliu.itx"
    version = "0.1.0"
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "com.diffplug.spotless")

    val libsCatalog = rootProject.extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
    val lombokCoord = libsCatalog.findLibrary("lombok").get()

    dependencies {
        "compileOnly"(lombokCoord)
        "annotationProcessor"(lombokCoord)
        "testCompileOnly"(lombokCoord)
        "testAnnotationProcessor"(lombokCoord)
    }

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
            googleJavaFormat("1.28.0")
            removeUnusedImports()
            trimTrailingWhitespace()
            endWithNewline()
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(
            listOf("-Amapstruct.defaultComponentModel=spring", "-parameters"),
        )
    }
}
