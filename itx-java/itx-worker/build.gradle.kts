plugins {
    id("itx.shadow-app-conventions")
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
