plugins {
    id("bisq.java-library")
}

dependencies {
    implementation("bisq:common:$version")
    implementation("bisq:security:$version")
    implementation("bisq:application:$version")
    implementation("bisq:evolution:$version")
    implementation("network:network:$version")

    implementation(libs.typesafe.config)
}
