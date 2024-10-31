plugins {
    id("bisq.java-library")
    id("bisq.gradle.maven_publisher.LocalMavenPublishPlugin")
}

dependencies {
    implementation(project(":persistence"))
    implementation(project(":i18n"))
    implementation(project(":security"))
    implementation(project(":identity"))
    implementation(project(":account"))
    implementation(project(":offer"))
    implementation(project(":contract"))
    implementation(project(":trade"))
    implementation(project(":bonded-roles"))
    implementation(project(":settings"))
    implementation(project(":user"))
    implementation(project(":chat"))
    implementation(project(":support"))
    implementation(project(":presentation"))

    implementation("network:network:$version")
    implementation("network:network-identity:$version")
    implementation("bitcoind:core:$version")
    implementation("wallets:wallet:$version")
    // implementation("wallets:electrum")
    // implementation("wallets:bitcoind")

    implementation(libs.google.gson)
    implementation(libs.typesafe.config)

    testImplementation(libs.testfx.junit5)
}
