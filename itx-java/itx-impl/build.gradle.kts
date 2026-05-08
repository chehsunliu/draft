plugins {
    `java-library`
    alias(libs.plugins.spring.dependency.management)
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:${libs.versions.springBoot.get()}")
    }
}

dependencies {
    api(project(":itx-contract"))

    api(libs.spring.boot.starter)
    api(libs.spring.boot.starter.data.jpa)

    api(libs.aws.sqs)
    runtimeOnly(libs.aws.sso)

    api(libs.amqp.client)

    runtimeOnly(libs.postgres)
    runtimeOnly(libs.mariadb)

    // RestClient (used by the email impl) ships with spring-boot-starter-web; we
    // only need the WebMVC autoconfig bits in the backend, so we declare web here
    // as compileOnly to access RestClient without forcing every consumer to embed
    // Tomcat.
    compileOnly(libs.spring.boot.starter.web)

    annotationProcessor(libs.mapstruct.processor)
    annotationProcessor(libs.lombok.mapstruct.binding)
    implementation(libs.mapstruct)
}
