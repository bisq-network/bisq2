plugins {
    id("bisq.java-library")
    id("bisq.protobuf")
}

dependencies {
    implementation(project(":persistence"))
    implementation(project(":security"))

    implementation("network:network-common")
    implementation("network:network")
    implementation("network:network-identity")

    implementation(libs.typesafe.config)
}
