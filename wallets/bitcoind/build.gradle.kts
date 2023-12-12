plugins {
    id("bisq.java-library")
    id("bisq.java-integration-tests")
    id("bisq.protobuf")
}

dependencies {
    implementation("bisq:persistence")

    implementation(project(":core"))
    implementation(project(":json-rpc"))

    implementation(libs.typesafe.config)
    implementation(libs.jeromq)

    integrationTestImplementation(project(":regtest"))
}