import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    `java-library`
    id("com.diffplug.spotless")
}

group = "io.github.chehsunliu.itx"
version = "0.1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
}

val libs = the<VersionCatalogsExtension>().named("libs")
val lombok = libs.findLibrary("lombok").get()
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
