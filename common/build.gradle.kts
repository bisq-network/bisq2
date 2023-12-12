plugins {
    id("bisq.java-conventions")
    id("bisq.protobuf")
}

dependencies {
    implementation(libs.typesafe.config)
    implementation(libs.annotations)
}
