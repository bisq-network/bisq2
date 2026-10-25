plugins {
    id("bisq.java-conventions")
}

dependencies {
    api(platform("bisq:platform:${libs.versions.bisq.get()}"))
}
