plugins {
    id("bisq.java-library")
    id("bisq.protobuf")
}

dependencies {
    implementation(project(":persistence"))
    implementation(project(":i18n"))
    implementation(project(":security"))
    implementation(project(":bonded-roles"))
    implementation("network:network")

    implementation(libs.bundles.jackson)
    implementation(libs.typesafe.config)
}
