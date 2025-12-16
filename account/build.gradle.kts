plugins {
    id("bisq.java-library")
    id("bisq.protobuf")
}

version = rootProject.version

dependencies {
    implementation(project(":i18n"))
    implementation(project(":persistence"))
    implementation(project(":security"))
    implementation(project(":identity"))
    implementation(project(":bonded-roles"))

    implementation("network:network:$version")

    implementation(libs.bundles.jackson)
}
