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
    implementation(project(":account"))
    implementation(project(":presentation"))
    implementation(project(":bonded-roles"))

    implementation("network:network:$version")
    implementation("network:network-identity:$version")

    implementation(libs.google.gson)
    implementation(libs.chimp.jsocks)
}
