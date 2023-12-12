plugins {
    id("bisq.java-library")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":bitcoind"))
    implementation(project(":json-rpc"))
    implementation(project(":process"))

    api(project(":process"))

    implementation(libs.assertj.core)
    implementation(libs.junit.jupiter)
}