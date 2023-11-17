plugins {
    id("bisq.java-library")
    alias(libs.plugins.openjfx)
}

javafx {
    version = "17.0.1"
    modules = listOf("javafx.controls", "javafx.media")
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
    implementation(project(":bisq_easy"))
    implementation(project(":application"))

    implementation("network:common")
    implementation("network:network")
    implementation("network:network-identity")

    implementation("wallets:electrum")
    implementation("wallets:bitcoind")

    implementation(libs.google.guava)
    implementation(libs.google.gson)
    implementation(libs.bundles.fontawesomefx)
    implementation(libs.bundles.fxmisc.libs)
    implementation(libs.typesafe.config)

    testImplementation(libs.testfx.junit5)
    testImplementation(libs.openjfx.monocle)
}
