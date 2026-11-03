plugins {
    id ("bisq.java-library")
    id ("bisq.protobuf")
}

version = rootProject.version

dependencies {
    implementation("bisq:common:$version")
    implementation("bisq:security:$version")

    implementation(libs.bouncycastle)
    implementation(libs.bundles.jackson)
}
