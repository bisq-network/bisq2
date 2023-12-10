plugins {
    id("bisq.java-library")
    application
}

application {
    mainClass.set("bisq.seed_node.SeedNodeApp")
}

dependencies {
    implementation("bisq:persistence")
    implementation("bisq:security")
    implementation("bisq:bonded-roles")
    implementation("bisq:application")
    implementation("bisq:identity")

    implementation("network:network-common")
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
