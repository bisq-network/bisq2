plugins {
    id("bisq.java-library")
}

dependencies {
    implementation(project(":presentation"))
    implementation(project(":persistence"))
    implementation(project(":settings"))

    implementation(libs.java.dev.jna)
}
