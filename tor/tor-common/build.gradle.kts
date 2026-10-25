plugins {
    id("bisq.java-conventions")
    id("bisq.java-integration-tests")
}

dependencies {
    api(platform(project(":tor-platform")))
}
