plugins {
    id("bisq.java-library")
    id("bisq.protobuf")
}

dependencies {
    implementation(project(":persistence"))
    implementation(project(":identity"))
    implementation(project(":user"))
    implementation(project(":security"))
    implementation(project(":account"))
    implementation(project(":offer"))

    implementation("network:network:$version")
    implementation("network:network-identity:$version")
}
