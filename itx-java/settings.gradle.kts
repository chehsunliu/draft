rootProject.name = "itx-java"

include(
    "itx-contract",
    "itx-impl",
    "itx-backend",
    "itx-worker",
)

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
