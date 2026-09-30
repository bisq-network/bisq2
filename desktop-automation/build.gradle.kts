plugins {
    id("bisq.java-library")
    alias(libs.plugins.openjfx)
}

javafx {
    version = "21.0.6"
    modules = listOf("javafx.controls", "javafx.media", "javafx.swing")
}

dependencies {
    implementation("bisq:desktop-automation-contract")
    implementation(libs.google.gson)
}
