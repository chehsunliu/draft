plugins {
    application
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(project(":itx-contract"))
    implementation(project(":itx-impl"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    runtimeOnly(libs.logback.classic)
    runtimeOnly(libs.logstash.encoder)
}

application {
    mainClass.set("io.github.chehsunliu.itx.worker.MainKt")
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveBaseName.set("itx-worker")
    archiveClassifier.set("")
    archiveVersion.set("")
    mergeServiceFiles()
    manifest { attributes["Main-Class"] = "io.github.chehsunliu.itx.worker.MainKt" }
}
