plugins {
    `kotlin-dsl`
}

dependencies {
    // Plugin classpath for convention plugins under src/main/kotlin/.
    implementation("com.diffplug.spotless:spotless-plugin-gradle:${libs.versions.spotless.get()}")
    implementation("com.gradleup.shadow:shadow-gradle-plugin:8.3.10")
}
