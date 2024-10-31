plugins {
    id("bisq.java-library")
    id("bisq.protobuf")
    id("bisq.gradle.maven_publisher.LocalMavenPublishPlugin")
}

dependencies {
    implementation(project(":persistence"))
    implementation(project(":security"))
    implementation(project(":i18n"))
    implementation(project(":identity"))
    implementation(project(":settings"))

    implementation("network:network-identity:$version")
    implementation("network:network:$version")

    implementation(libs.google.gson)
    implementation(libs.typesafe.config)
    implementation(libs.bundles.jackson)
}
