plugins {
    id("bisq.java-library")
    id("bisq.java-integration-tests")
    id("bisq.protobuf")
}

// this one needs specific setup otherwise gives "invalid source release" error
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    implementation("bisq:persistence")

    implementation("bitcoind:core")
    implementation("bitcoind:bitcoind")
    implementation("bitcoind:json-rpc")

    implementation(libs.typesafe.config)
}