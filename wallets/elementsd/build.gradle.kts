plugins {
    id("bisq.java-library")
    id("bisq.protobuf")
    id("bisq.java-integration-tests")
}

dependencies {
    implementation("bisq:persistence")

    implementation(project(":core"))
    implementation(project(":bitcoind"))
    implementation(project(":json-rpc"))

    integrationTestImplementation(project(":regtest"))
}