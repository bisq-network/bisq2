plugins {
    id("bisq.java-library")
    id("bisq.protobuf")
    application
}

repositories {
    maven {
        url = uri("https://jitpack.io")
    }
}

application {
    mainClass.set("bisq.oracle_node.OracleNodeApp")
}

dependencies {
    implementation("bisq:persistence")
    implementation("bisq:security")
    implementation("bisq:identity")
    implementation("bisq:bonded-roles")
    implementation("bisq:user")
    implementation("bisq:application")

    implementation("network:network-common")
    implementation("network:network")
    implementation("network:network-identity")

    implementation(libs.google.gson)
    implementation(libs.typesafe.config)
    implementation(libs.bundles.jackson)
}

tasks {
    distZip {
        enabled = false
    }

    distTar {
        enabled = false
    }
}
