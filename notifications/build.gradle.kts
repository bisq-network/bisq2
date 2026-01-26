plugins {
    id("bisq.java-library")
}

dependencies {
    implementation(project(":persistence"))
    implementation(project(":i18n"))
    implementation("network:network")

    implementation(libs.typesafe.config)
}
