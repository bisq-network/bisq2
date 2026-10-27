import java.util.Properties

plugins {
    id("bisq.java-library")
    id("bisq.protobuf")
    id("bisq.java-integration-tests")
}

version = rootProject.version

// The Tor build in the Git submodule tor/ has its own version. The published POM must name it, because the Maven
// publisher adds the version of Bisq 2 to dependencies without a version.
val torBuildVersion: String = Properties().apply {
    rootProject.file("../tor/gradle.properties").inputStream().use { load(it) }
}.getProperty("version") ?: throw GradleException("Tor build version is not defined in tor/gradle.properties")

dependencies {
    implementation("bisq:common")
    implementation("bisq:security")
    implementation("bisq:persistence")
    implementation("bisq:i18n")

    implementation(project(":network-identity"))
    implementation(project(":i2p"))
    implementation("tor:tor:$torBuildVersion")

    implementation(libs.bouncycastle)
    implementation(libs.failsafe)
    implementation(libs.typesafe.config)

    implementation(libs.apache.httpcomponents.core)
    implementation(libs.apache.httpcomponents.httpclient)
    api("tor:jsocks:$torBuildVersion")
    implementation(libs.bundles.i2p)
    implementation(libs.bundles.jackson)

    integrationTestImplementation(libs.mockito)
}
