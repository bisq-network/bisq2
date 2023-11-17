plugins {
    id("bisq.java-library")
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
    implementation(project(":bonded_roles"))
    implementation(project(":settings"))
    implementation(project(":user"))
    implementation(project(":chat"))
    implementation(project(":support"))
    implementation(project(":presentation"))

    implementation("network:network")
    implementation("wallets:electrum")
    implementation("wallets:bitcoind")

    implementation(libs.google.guava)
    implementation(libs.google.gson)
    implementation(libs.typesafe.config)

    testImplementation(libs.testfx.junit5)
}
