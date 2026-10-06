plugins {
    id("bisq.java-library")
    id("bisq.java-integration-tests")
}

// Black-box tests of networks with nodes of two versions. The tests start the api-app and seed node distributions of
// both versions as processes and use only the API and the logs of the nodes. tools/compat/run-mixed-network.sh builds
// the distributions and runs the task integrationTest. The build of the module only compiles the tests.
//
// No API of the nodes publishes the alerts of a security manager. AlertAcrossVersionsTest therefore starts the program
// of the source set alertPublisher as a node of this checkout. Only this source set uses the code of Bisq, so the
// tests themselves stay limited to the API and the logs.
val alertPublisher: SourceSet by sourceSets.creating

dependencies {
    integrationTestImplementation(libs.jackson.databind)

    "alertPublisherImplementation"(project(":api-app"))
    "alertPublisherImplementation"("bisq:application")
    "alertPublisherImplementation"("bisq:bonded-roles")
    "alertPublisherImplementation"("bisq:common")
    "alertPublisherImplementation"("bisq:java-se")
    "alertPublisherImplementation"("bisq:persistence")
    "alertPublisherImplementation"("bisq:security")
    "alertPublisherImplementation"("network:network")
    "alertPublisherImplementation"(libs.slf4j.api)
}

val alertPublisherClasspath: FileCollection = alertPublisher.runtimeClasspath

tasks.named<Test>("integrationTest") {
    listOf("bisq.e2e.newTree", "bisq.e2e.oldTree", "bisq.e2e.oldVersion", "bisq.e2e.workDir", "bisq.e2e.libfaketime").forEach { name ->
        val value = providers.gradleProperty(name)
        inputs.property(name, value.orElse(""))
        systemProperty(name, value.orElse("").get())
    }
    // The tests start the alert publisher with this classpath, so Gradle builds it before the tests run
    inputs.files(alertPublisherClasspath)
        .withPropertyName("alertPublisherClasspath")
        .withNormalizer(ClasspathNormalizer::class)
    jvmArgumentProviders.add(CommandLineArgumentProvider {
        listOf("-Dbisq.e2e.alertPublisherClasspath=" + alertPublisherClasspath.asPath)
    })
    // The result depends on processes and files outside of Gradle
    outputs.upToDateWhen { false }
    // Each test class starts its own network. Several networks at the same time would need too much memory.
    maxParallelForks = 1
    testLogging {
        events("passed", "skipped", "failed", "standardError")
        showStandardStreams = false
    }
}

// The build compiles the tests and the alert publisher, so that the CI build finds compile errors, but it does not run
// the tests
tasks.named("assemble") {
    dependsOn("integrationTestClasses", "alertPublisherClasses")
}
