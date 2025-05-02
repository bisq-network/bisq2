plugins {
    id("bisq.java-library")
}

dependencies {
    implementation("bitcoind:core")
    implementation("bitcoind:bitcoind")

    implementation(libs.assertj.core)
    implementation(libs.junit.jupiter)
}