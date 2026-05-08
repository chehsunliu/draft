plugins {
    `java-library`
}

dependencies {
    // Pure-API module: no Spring, no infra. Plain interfaces and value objects only.
    // Jackson annotations are pulled in so the queue message envelope can declare its
    // polymorphic-type discriminator next to the type itself, the way itx-rs does with
    // serde's #[serde(tag = "type")].
    api(libs.jackson.annotations)

    annotationProcessor(libs.mapstruct.processor)
    annotationProcessor(libs.lombok.mapstruct.binding)
    implementation(libs.mapstruct)
}
