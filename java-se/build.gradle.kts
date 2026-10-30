plugins {
    id("bisq.java-library")
}

dependencies {
    implementation("bisq:common")
    implementation("bisq:security")
    implementation("bisq:application")
    implementation("bisq:evolution")
    implementation("network:network")

    implementation(libs.typesafe.config)
}
