plugins {
    id("bisq.java-library")
    id("bisq.protobuf")
}

repositories {
    maven {
        url = uri("https://jitpack.io")
    }
}

dependencies {
    implementation("bisq:persistence")
    implementation("bisq:security")
    implementation("bisq:identity")
    implementation("bisq:bonded-roles")
    implementation("bisq:user")

    implementation("network:network-common")
    implementation("network:network")
    implementation("network:network-identity")

    implementation(libs.google.gson)
    implementation(libs.typesafe.config)
    implementation(libs.bundles.jackson)
}
