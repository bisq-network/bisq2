plugins {
    id("bisq.java-library")
    id("bisq.protobuf")
    id("bisq.java-integration-tests")
}

dependencies {
    implementation("bisq:persistence")

    implementation("bitcoind:core")
    implementation("bitcoind:bitcoind")
    implementation("bitcoind:json-rpc")

    integrationTestImplementation("bitcoind:regtest")
}