import kotlin.jvm.optionals.getOrNull

plugins {
    id("bisq.java-conventions")
}

val bisqVersion = extensions.getByType<VersionCatalogsExtension>().named("libs").findVersion("bisq").getOrNull()

dependencies {
    api(platform("bisq:platform:$bisqVersion"))
    implementation("bisq:common")
}