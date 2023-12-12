plugins {
    id("bisq.java-library")
    application
}

application {
    mainClass.set("bisq.oracle_node_app.OracleNodeApp")
}

dependencies {
    implementation("bisq:persistence")
    implementation("bisq:security")
    implementation("bisq:identity")
    implementation("bisq:bonded-roles")
    implementation("bisq:application")

    implementation(project(":oracle-node"))

    implementation("network:network-common")
    implementation("network:network-identity")
    implementation("network:network")

    implementation(libs.google.gson)
    implementation(libs.typesafe.config)
}

tasks {
    distZip {
        enabled = false
    }

    distTar {
        enabled = false
    }
}
