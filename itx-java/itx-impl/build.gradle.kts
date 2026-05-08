dependencies {
    api(project(":itx-contract"))

    api(libs.jackson.databind)
    api(libs.jackson.jsr310)

    api(libs.hikari)
    runtimeOnly(libs.postgres)
    runtimeOnly(libs.mariadb)

    api(libs.aws.sqs)
    runtimeOnly(libs.aws.sso)

    api(libs.amqp.client)

    api(libs.slf4j.api)
}
