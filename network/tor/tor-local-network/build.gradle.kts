plugins {
    id("bisq.java-conventions")
    id("bisq.java-integration-tests")
    application
}

application {
    mainClass.set("bisq.network.tor.local_network.Main")
}

dependencies {
    api(platform("bisq:platform:${libs.versions.bisq.get()}"))

    implementation(project(":tor-common"))
    implementation(project(":tor"))
    implementation(project(":jtorctl"))
}
