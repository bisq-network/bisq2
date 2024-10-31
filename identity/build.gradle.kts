plugins {
    id("bisq.java-library")
    id("bisq.protobuf")
    id("bisq.gradle.maven_publisher.LocalMavenPublishPlugin")
}

dependencies {
    implementation(project(":persistence"))
    implementation(project(":security"))

    implementation("network:network:$version")
    implementation("network:network-identity:$version")

    implementation(libs.typesafe.config)
}
