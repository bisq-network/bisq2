plugins {
    id ("bisq.java-library")
    id ("bisq.protobuf")
}

dependencies {
    implementation("bisq:common")
    implementation("bisq:security")

    implementation(libs.bouncycastle)
}
