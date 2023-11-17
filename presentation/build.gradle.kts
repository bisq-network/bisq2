plugins {
    id("bisq.java-library")
    id("bisq.protobuf")
}

dependencies {
    implementation(project(":persistence"))
    implementation(project(":i18n"))

    implementation(libs.google.guava)
    implementation("net.java.dev.jna:jna:5.13.0")
}
