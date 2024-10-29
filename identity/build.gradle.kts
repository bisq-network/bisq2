plugins {
    id("bisq.java-library")
    id("bisq.protobuf")
    id("bisq.gradle.maven_publisher.LocalMavenPublishPlugin")
}

dependencies {
    implementation(project(":persistence"))
    implementation(project(":security"))

    implementation("network:network")
    implementation("network:network-identity")

    implementation(libs.typesafe.config)
}
