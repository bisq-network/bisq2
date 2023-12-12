plugins {
    id("bisq.java-library")
    id("bisq.protobuf")
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://jitpack.io")
    }
}

dependencies {
    implementation(project(":i18n"))
    implementation(project(":persistence"))
    implementation(project(":security"))
    implementation(project(":identity"))
    implementation(project(":user"))
    implementation(project(":offer"))
    implementation(project(":settings"))
    implementation(project(":presentation"))

    implementation("network:network")
    implementation("network:network-identity")

    implementation(libs.chimp.jsocks)
    implementation(libs.google.gson)
    implementation(libs.typesafe.config)
}
