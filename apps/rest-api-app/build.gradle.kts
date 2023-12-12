plugins {
    id("bisq.java-library")
    application
}

application {
    mainClass.set("bisq.rest_api.RestApiApp")
}

dependencies {
    implementation("bisq:persistence")
    implementation("bisq:i18n")
    implementation("bisq:security")
    implementation("bisq:identity")
    implementation("bisq:account")
    implementation("bisq:offer")
    implementation("bisq:contract")
    implementation("bisq:trade")
    implementation("bisq:bonded-roles")
    implementation("bisq:settings")
    implementation("bisq:user")
    implementation("bisq:chat")
    implementation("bisq:support")
    implementation("bisq:presentation")
    implementation("bisq:bisq-easy")
    implementation("bisq:application")

    implementation("network:network")
    implementation("wallets:electrum")
    implementation("wallets:bitcoind")

    implementation(libs.typesafe.config)
    implementation(libs.bundles.glassfish.jersey)
    implementation(libs.bundles.jackson)

    implementation(libs.swagger.jaxrs2.jakarta)
}

tasks {
    distZip {
        enabled = false
    }

    distTar {
        enabled = false
    }
}
