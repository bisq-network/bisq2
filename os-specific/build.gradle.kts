plugins {
    id("bisq.java-library")
}

dependencies {
    implementation(project(":notifications"))
    implementation(project(":persistence"))
    implementation(project(":settings"))

    implementation(libs.java.dev.jna)
}
