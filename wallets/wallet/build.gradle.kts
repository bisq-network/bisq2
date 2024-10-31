plugins {
    id("bisq.java-library")
    id("bisq.java-integration-tests")
    id("bisq.protobuf")
}

version = rootProject.version

dependencies {
    implementation("bisq:persistence")

    implementation("bitcoind:core:$version")
    implementation("bitcoind:bitcoind:$version")
    implementation("bitcoind:json-rpc")

    implementation(libs.typesafe.config)
}