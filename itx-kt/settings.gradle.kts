rootProject.name = "itx-kt"

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
