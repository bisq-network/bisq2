plugins {
    id("bisq.java-library")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(22))
    }
}

dependencies {
    implementation("bitcoind:core")
    implementation("bitcoind:bitcoind")

    implementation(libs.assertj.core)
    implementation(libs.junit.jupiter)
}