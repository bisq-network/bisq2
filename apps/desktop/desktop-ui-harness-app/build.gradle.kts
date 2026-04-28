plugins {
    id("bisq.java-library")
    application
    alias(libs.plugins.openjfx)
}

application {
    mainClass.set("bisq.desktop_ui_harness_app.DesktopUiHarnessApp")
}

javafx {
    version = "21.0.6"
    modules = listOf("javafx.controls", "javafx.media", "javafx.swing")
}

dependencies {
    implementation("bisq:desktop-automation")
    implementation("bisq:desktop-automation-contract")
    implementation(project(":desktop"))
    implementation(project(":desktop-app"))

    testImplementation("bisq:chat")
    testImplementation("bisq:i18n")
}

tasks {
    distZip {
        enabled = false
    }

    distTar {
        enabled = false
    }
}
