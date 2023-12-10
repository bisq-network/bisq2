plugins {
    id("bisq.java-library")
    id("bisq.protobuf")
}

dependencies {
    implementation(project(":persistence"))
    implementation(project(":i18n"))
    implementation(project(":settings"))

    implementation(libs.google.guava)
    implementation(libs.java.dev.jna)
}
