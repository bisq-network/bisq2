plugins {
    id("bisq.java-conventions")
    id("bisq.java-integration-tests")
}

dependencies {
    api(platform("bisq:platform:${libs.versions.bisq.get()}"))
}
