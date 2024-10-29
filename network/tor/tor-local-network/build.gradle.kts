plugins {
    id("bisq.java-library")
    id("bisq.java-integration-tests")
    application
}

application {
    mainClass.set("bisq.network.tor.local_network.Main")
}

dependencies {
    implementation(project(":tor-common"))
    implementation(project(":tor"))
    implementation(libs.chimp.jtorctl)
}