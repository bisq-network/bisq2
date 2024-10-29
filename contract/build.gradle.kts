plugins {
    id("bisq.java-library")
    id("bisq.protobuf")
    id("bisq.gradle.maven_publisher.LocalMavenPublishPlugin")
}

dependencies {
    implementation(project(":persistence"))
    implementation(project(":identity"))
    implementation(project(":user"))
    implementation(project(":security"))
    implementation(project(":account"))
    implementation(project(":offer"))

    implementation("network:network")
    implementation("network:network-identity")
}
