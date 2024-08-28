plugins {
    id("bisq.java-library")
    id("bisq.gradle.packaging.ProGuardPlugin") version "0.1.0"
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
    implementation("bisq:user")

    implementation("network:network-common")
    implementation("network:network")
    implementation("network:network-identity")

    implementation(libs.typesafe.config)
    implementation(libs.google.gson)
}

tasks {
    installDist {
        dependsOn("proguardTask") // Ensure that ProGuard runs before installation
    }
    distZip {
        enabled = false
    }

    distTar {
        enabled = false
    }
}
