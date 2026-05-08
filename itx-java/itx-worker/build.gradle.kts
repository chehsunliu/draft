plugins {
    application
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(project(":itx-contract"))
    implementation(project(":itx-impl"))

    implementation(libs.jackson.databind)
    implementation(libs.jackson.jsr310)

    runtimeOnly(libs.logback.classic)
    runtimeOnly(libs.logstash.encoder)
}

application {
    mainClass.set("io.github.chehsunliu.itx.worker.Main")
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveBaseName.set("itx-worker")
    archiveClassifier.set("")
    archiveVersion.set("")
    mergeServiceFiles()
    manifest { attributes["Main-Class"] = "io.github.chehsunliu.itx.worker.Main" }
}
