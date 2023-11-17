plugins {
    id("bisq.java-library")
    id("bisq.protobuf")
}

dependencies {
    implementation(project(":persistence"))
    implementation(project(":security"))

    implementation("network:common")
    implementation("network:network")
    implementation("network:network-identity")

    implementation(libs.google.guava)
    implementation(libs.typesafe.config)
}
