plugins {
    id("bisq.java-library")
    id("bisq.java-integration-tests")
    application
}

application {
    mainClass.set("bisq.tor.local_network.Main")
}

dependencies {
    implementation(project(":tor-common"))
    implementation(libs.chimp.jtorctl)
}