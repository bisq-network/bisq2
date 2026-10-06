plugins {
    id("bisq.java-library")
}

dependencies {
    implementation(project(":i18n"))
    implementation(project(":persistence"))
    implementation(project(":security"))
    implementation(project(":account"))
    implementation(project(":settings"))
    implementation(project(":user"))
    implementation(project(":burningman"))
    implementation(project(":chat"))
    implementation(project(":support"))
    implementation(project(":bonded-roles"))
    implementation(project(":offer"))
    implementation(project(":trade"))

    implementation("network:network:$version")

    implementation(libs.typesafe.config)

    // StorageRequestsOfEarlierVersionTest parses serialized network messages
    testImplementation(libs.protobuf.java)
}

// ResolverConfigTest parses ResolverConfig.java as text, so source-only edits such as wrapping a call across lines or
// changing a comment have to invalidate the task even though they leave the compiled class unchanged.
tasks.test {
    inputs.file("src/main/java/bisq/application/ResolverConfig.java")
        .withPropertyName("resolverConfigSource")
        .withPathSensitivity(PathSensitivity.RELATIVE)

    // The steps of the compatibility chain need the files which the code of a released version creates.
    // tools/compat/run-chain.sh runs them with the task compatTest.
    exclude("bisq/application/compat/**")
}

val compatExchangeDir = providers.gradleProperty("bisq.compat.exchangeDir")

tasks.register<Test>("compatTest") {
    description = "Runs the steps of the compatibility chain with the released version. See tools/compat/README.md."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform()
    include("bisq/application/compat/**")
    inputs.property("exchangeDir", compatExchangeDir.orElse(""))
    systemProperty("bisq.compat.exchangeDir", compatExchangeDir.orElse("").get())
    // The result depends on the files in the exchange directory, which Gradle does not track
    outputs.upToDateWhen { false }
}
