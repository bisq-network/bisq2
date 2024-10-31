plugins {
    id("bisq.java-library")
    id("bisq.protobuf")
    id("bisq.gradle.maven_publisher.LocalMavenPublishPlugin")
}

dependencies {
    implementation(project(":persistence"))
    implementation(project(":i18n"))
    implementation(project(":security"))
    implementation(project(":identity"))
    implementation(project(":user"))
    implementation(project(":account"))
    implementation(project(":offer"))
    implementation(project(":contract"))
    implementation(project(":support"))
    implementation(project(":chat"))
    implementation(project(":settings"))
    implementation(project(":presentation"))
    implementation(project(":bonded-roles"))

    implementation("network:network:$version")
    implementation("network:network-identity:$version")

    implementation(libs.typesafe.config)
}
