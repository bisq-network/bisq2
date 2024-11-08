import kotlin.jvm.optionals.getOrNull

plugins {
    id("bisq.java-conventions")
}

val bisqVersion = extensions.getByType<VersionCatalogsExtension>().named("libs").findVersion("bisq").getOrNull()
//println("bisq.java-library: BISQ version for platform $bisqVersion")

dependencies {
    api(platform("bisq:platform:$bisqVersion"))
    implementation("bisq:common")
}