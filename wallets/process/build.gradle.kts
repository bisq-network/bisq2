plugins {
    id("bisq.java-library")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":bitcoind"))

    implementation(libs.assertj.core)
    implementation(libs.junit.jupiter)
}