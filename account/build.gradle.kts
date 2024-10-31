plugins {
    id("bisq.java-library")
    id("bisq.protobuf")
    id("bisq.gradle.maven_publisher.LocalMavenPublishPlugin")
}

dependencies {
    implementation(project(":i18n"))
    implementation(project(":persistence"))
    implementation(project(":security"))
    implementation(project(":identity"))

    implementation("network:network:$version")
}
