plugins {
    id("bisq.java-library")
    id("bisq.protobuf")
    id("bisq.java-integration-tests")
}

version = rootProject.version

dependencies {
    annotationProcessor(project(":storage-policy-processor"))
    testAnnotationProcessor(project(":storage-policy-processor"))
    implementation("bisq:common")
    implementation("bisq:security")
    implementation("bisq:persistence")
    implementation("bisq:i18n")

    implementation(project(":network-identity"))
    implementation(project(":i2p"))
    implementation("tor:tor:$version")

    implementation(libs.bouncycastle)
    implementation(libs.failsafe)
    implementation(libs.typesafe.config)

    implementation(libs.apache.httpcomponents.httpclient)
    api("tor:jsocks:$version")
    implementation(libs.bundles.i2p)
    implementation(libs.bundles.jackson)

    integrationTestImplementation(libs.mockito)
}
