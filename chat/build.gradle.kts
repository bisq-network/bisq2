plugins {
    id("bisq.java-library")
    id("bisq.protobuf")
}

dependencies {
    implementation(project(":i18n"))
    implementation(project(":persistence"))
    implementation(project(":security"))
    implementation(project(":identity"))
    implementation(project(":account"))
    implementation(project(":user"))
    implementation(project(":offer"))
    implementation(project(":settings"))
    implementation(project(":presentation"))
    implementation(project(":bonded-roles"))

    implementation("network:network:$version")
    implementation("network:network-identity:$version")

    implementation(libs.chimp.jsocks)
    implementation(libs.google.gson)
    implementation(libs.typesafe.config)
}
