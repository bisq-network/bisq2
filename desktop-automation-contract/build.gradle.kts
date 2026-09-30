plugins {
    id("bisq.java-library")
    alias(libs.plugins.openjfx)
}

javafx {
    version = "21.0.11"
    modules = listOf("javafx.graphics")
}
