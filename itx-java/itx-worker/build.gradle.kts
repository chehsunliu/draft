plugins {
    application
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

dependencies {
    implementation(project(":itx-contract"))
    implementation(project(":itx-impl"))

    implementation(libs.spring.boot.starter)
    // RestClient (used by the email impl pulled in via itx-impl) lives in spring-web.
    implementation(libs.spring.boot.starter.web) {
        // Workers don't serve HTTP, but we want RestClient. Excluding the embedded
        // server keeps the worker from binding to a port.
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
    }

    runtimeOnly(libs.postgres)
    runtimeOnly(libs.mariadb)

    annotationProcessor(libs.mapstruct.processor)
    annotationProcessor(libs.lombok.mapstruct.binding)
    implementation(libs.mapstruct)
}

application {
    mainClass.set("io.github.chehsunliu.itx.worker.WorkerApplication")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("itx-worker.jar")
    mainClass.set("io.github.chehsunliu.itx.worker.WorkerApplication")
}
