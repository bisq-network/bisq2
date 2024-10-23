plugins {
    id("bisq.java-library")
}

dependencies {
    implementation("bisq:common")
    implementation("bisq:security")
    implementation("bisq:application")
    implementation("bisq:evolution")

    implementation(libs.typesafe.config)
}
