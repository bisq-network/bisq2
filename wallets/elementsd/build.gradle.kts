plugins {
    id("bisq.java-library")
    id("bisq.protobuf")
    id("bisq.java-integration-tests")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(22))
    }
}

dependencies {
    implementation("bisq:persistence")

    implementation("bitcoind:core")
    implementation("bitcoind:bitcoind")
    implementation("bitcoind:json-rpc")
    implementation("wallets:wallet")

    integrationTestImplementation("bitcoind:regtest")
}
