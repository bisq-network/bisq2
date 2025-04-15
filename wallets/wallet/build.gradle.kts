plugins {
    id("bisq.java-library")
    id("bisq.java-integration-tests")
    id("bisq.protobuf")
}

dependencies {
    implementation("bisq:persistence")

    implementation("bitcoind:core")
    implementation("bitcoind:bitcoind")
    implementation("bitcoind:json-rpc")

    implementation(libs.typesafe.config)
}