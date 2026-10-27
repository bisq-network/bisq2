plugins {
    id("bisq.java-library")
    id("bisq.javafx-tests")
    application
    alias(libs.plugins.openjfx)
}

application {
    mainClass.set("bisq.desktop_ui_harness_app.DesktopUiHarnessApp")
}

javafx {
    version = libs.versions.javafx.get()
    modules = listOf("javafx.controls", "javafx.media", "javafx.swing")
}

dependencies {
    implementation("bisq:desktop-automation")
    implementation("bisq:desktop-automation-contract")
    implementation(project(":desktop"))
    implementation(project(":desktop-app"))

    testImplementation("bisq:chat")
    testImplementation("bisq:i18n")
    testImplementation("bisq:persistence")
    testImplementation("bisq:settings")
}

tasks {
    distZip {
        enabled = false
    }

    distTar {
        enabled = false
    }
}
