plugins {
    id("bisq.java-library")
    alias(libs.plugins.openjfx)
}

javafx {
    version = libs.versions.javafx.get()
    modules = listOf("javafx.graphics")
}
