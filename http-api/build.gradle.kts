plugins {
    id("bisq.java-library")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
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
    implementation("network:network-identity")
    implementation("bitcoind:core")
    implementation("wallets:wallet")

    implementation(libs.typesafe.config)
    implementation(libs.bundles.rest.api.libs)
    implementation(libs.bundles.websocket.libs)
}
