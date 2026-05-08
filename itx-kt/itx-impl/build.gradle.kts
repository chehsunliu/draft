plugins {
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(project(":itx-contract"))

    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.coroutines.jdk8)
    api(libs.kotlinx.serialization.json)

    api(libs.exposed.core)
    api(libs.exposed.jdbc)
    api(libs.exposed.java.time)
    api(libs.hikari)
    runtimeOnly(libs.postgres)
    runtimeOnly(libs.mariadb)

    api(libs.aws.sqs)
    runtimeOnly(libs.aws.sso)

    api(libs.amqp.client)

    api(libs.ktor.client.core)
    api(libs.ktor.client.cio)
    api(libs.ktor.client.content.negotiation)
    api(libs.ktor.serialization.kotlinx.json)

    api(libs.slf4j.api)
}
