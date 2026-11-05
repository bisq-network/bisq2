plugins {
    id("bisq.java-library")
    id("bisq.javafx-tests")
    alias(libs.plugins.openjfx)
}

javafx {
    version = libs.versions.javafx.get()
    modules = listOf("javafx.controls", "javafx.media", "javafx.swing")
}

dependencies {
    implementation("bisq:desktop-automation-contract:$version")
    implementation(libs.google.gson)
}
