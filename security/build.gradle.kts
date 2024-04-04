plugins {
    id("bisq.java-library")
    id("bisq.protobuf")
}

dependencies {
    implementation(project(":persistence"))

    implementation(libs.bouncycastle)
    implementation(libs.bouncycastle.pg)
    implementation(libs.typesafe.config)

    testImplementation(libs.apache.commons.lang)
}
