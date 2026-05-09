import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    application
    id("com.gradleup.shadow")
    id("itx.serialization-conventions")
}

tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("")
    archiveVersion.set("")
    mergeServiceFiles()
}
