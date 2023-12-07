plugins {
    id ("bisq.java-library")
    id ("bisq.protobuf")
}

dependencies {
    implementation(project(":network-common"))
    implementation("bisq:security")

    implementation(libs.bouncycastle)
    implementation(libs.google.guava)
}
