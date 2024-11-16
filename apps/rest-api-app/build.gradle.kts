plugins {
    id("bisq.java-library")
    application
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(22))
    }
}

application {
    mainClass.set("bisq.rest_api.RestApiApp")
}

dependencies {
    implementation("bisq:persistence")
    implementation("bisq:java-se")
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
    implementation("bisq:evolution")
    implementation("bisq:os-specific")

    implementation("network:network")
    implementation("bitcoind:core")
    implementation("wallets:wallet")

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
