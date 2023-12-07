plugins {
    id("bisq.java-conventions")
}

dependencies {
    api(platform("bisq:platform"))
    implementation("bisq:common")
}