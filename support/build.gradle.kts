plugins {
    id("bisq.java-library")
    id("bisq.protobuf")
}

dependencies {
    implementation(project(":persistence"))
    implementation(project(":security"))
    implementation(project(":i18n"))
    implementation(project(":identity"))
    implementation(project(":user"))
    implementation(project(":chat"))
    implementation(project(":offer"))
    implementation(project(":contract"))
    implementation(project(":bonded_roles"))

    implementation("network:network")
    implementation("network:network-identity")

    implementation(libs.google.gson)
    implementation(libs.google.guava)
    implementation(libs.typesafe.config)
}
