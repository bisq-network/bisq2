plugins {
    id("bisq.java-library")
    id("bisq.protobuf")
    id("bisq.java-integration-tests")
}

dependencies {
    implementation("bisq:security")
    implementation("bisq:persistence")

    implementation(project(":network-common"))
    implementation(project(":network-identity"))
    implementation(project(":i2p"))
    implementation("tor:tor")

    implementation(libs.bouncycastle)
    implementation(libs.failsafe)
    implementation(libs.typesafe.config)

    implementation(libs.apache.httpcomponents.httpclient)
    implementation(libs.chimp.jsocks)

    integrationTestImplementation(libs.mockito)
}
