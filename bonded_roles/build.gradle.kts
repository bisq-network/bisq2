plugins {
    id("bisq.java-library")
    id("bisq.protobuf")
}

dependencies {
    implementation(project(":persistence"))
    implementation(project(":security"))
    implementation(project(":i18n"))
    implementation(project(":identity"))

    implementation("network:network-common")
    implementation("network:network-identity")
    implementation("network:network")

    implementation(libs.google.gson)
    implementation(libs.typesafe.config)
    implementation(libs.bundles.jackson)
}
