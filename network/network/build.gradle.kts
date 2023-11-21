plugins {
    id("bisq.java-library")
    id("bisq.protobuf")
    id("bisq.java-integration-tests")
}

dependencies {
    implementation("bisq:security")
    implementation("bisq:persistence")

    implementation(project(":common"))
    implementation(project(":network-identity"))
    implementation(project(":i2p"))
    implementation("tor:tor")

    implementation(libs.bouncycastle)
    implementation(libs.failsafe)
    implementation(libs.google.guava)
    implementation(libs.typesafe.config)

    implementation(libs.apache.httpcomponents.httpclient)
    implementation(libs.chimp.jsocks)

    testImplementation(libs.mockito)

    integrationTestCompileOnly(libs.lombok)
    integrationTestAnnotationProcessor(libs.lombok)

    integrationTestImplementation(libs.mockito)
    integrationTestImplementation(libs.logback.core)
    integrationTestImplementation(libs.logback.classic)
    integrationTestImplementation(libs.slf4j.api)
}
