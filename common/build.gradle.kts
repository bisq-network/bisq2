plugins {
    id("bisq.java-conventions")
    id("bisq.java-integration-tests")
    id("bisq.protobuf")
}

dependencies {
    implementation(libs.typesafe.config)
    implementation(libs.annotations)
}
