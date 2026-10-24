plugins {
    id("bisq.java-library")
    id("bisq.javafx-tests")
    alias(libs.plugins.openjfx)
}

javafx {
    version = "21.0.13"
    modules = listOf("javafx.controls", "javafx.media", "javafx.swing")
}

dependencies {
    implementation("bisq:desktop-automation-contract")
    implementation(libs.google.gson)
}
