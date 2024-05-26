plugins {
    id("bisq.java-library")
    alias(libs.plugins.openjfx)
}

javafx {
    version = "21.0.3"
    modules = listOf("javafx.controls", "javafx.media")
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

    implementation("network:network-common")
    implementation("network:network")
    implementation("network:network-identity")

    implementation("wallets:electrum")
    implementation("wallets:bitcoind")

    implementation(libs.google.gson)
    implementation(libs.bundles.fontawesomefx)
    implementation(libs.bundles.fxmisc.libs)
    implementation(libs.typesafe.config)

    testImplementation(libs.testfx.junit5)
    testImplementation(libs.openjfx.monocle)
}
