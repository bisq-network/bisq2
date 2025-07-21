plugins {
    id("bisq.java-library")
    id("bisq.protobuf")
    application
}

application {
    mainClass.set("bisq.network_lab.NetworkLabApp")
}

dependencies {
    implementation("bisq:persistence")
    implementation("bisq:java-se")
    implementation("bisq:security")
    implementation("bisq:bonded-roles")
    implementation("bisq:identity")
    implementation("bisq:user")
    implementation("bisq:application")
    implementation("bisq:evolution")

    implementation(project(":seed-node-app"))

    implementation("network:network")
    implementation("network:network-identity")

    implementation(libs.typesafe.config)
    implementation(libs.google.gson)
}

tasks {
    distZip {
        enabled = false
    }

    distTar {
        enabled = false
    }
}
