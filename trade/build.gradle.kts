plugins {
    id("bisq.java-library")
    id("bisq.protobuf")
}

dependencies {
    implementation(project(":persistence"))
    implementation(project(":i18n"))
    implementation(project(":security"))
    implementation(project(":identity"))
    implementation(project(":user"))
    implementation(project(":account"))
    implementation(project(":offer"))
    implementation(project(":contract"))
    implementation(project(":support"))
    implementation(project(":chat"))
    implementation(project(":settings"))
    implementation(project(":presentation"))
    implementation(project(":bonded_roles"))

    implementation("network:network")
    implementation("network:network-identity")

    implementation(libs.google.guava)
    implementation(libs.typesafe.config)
}
