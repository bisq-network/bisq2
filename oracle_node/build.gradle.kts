plugins {
    id("bisq.java-library")
    id("bisq.protobuf")
}

repositories {
    maven {
        url = uri("https://jitpack.io")
    }
}

dependencies {
    implementation(project(":persistence"))
    implementation(project(":security"))
    implementation(project(":identity"))
    implementation(project(":bonded_roles"))
    implementation(project(":user"))

    implementation("network:network-common")
    implementation("network:network")
    implementation("network:network-identity")

    implementation(libs.google.gson)
    implementation(libs.google.guava)
    implementation(libs.typesafe.config)
    implementation(libs.bundles.jackson)
}
