plugins {
    application
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

dependencies {
    implementation(project(":itx-contract"))
    implementation(project(":itx-impl"))

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.validation)

    runtimeOnly(libs.postgres)
    runtimeOnly(libs.mariadb)

    annotationProcessor(libs.mapstruct.processor)
    annotationProcessor(libs.lombok.mapstruct.binding)
    implementation(libs.mapstruct)
}

application {
    mainClass.set("io.github.chehsunliu.itx.backend.BackendApplication")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("itx-backend.jar")
    mainClass.set("io.github.chehsunliu.itx.backend.BackendApplication")
}
