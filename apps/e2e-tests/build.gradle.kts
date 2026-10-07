plugins {
    id("bisq.java-library")
    id("bisq.java-integration-tests")
}

// Black-box tests of networks with nodes of two versions. The tests start the api-app and seed node distributions of
// both versions as processes and use only the API and the logs of the nodes. tools/compat/run-mixed-network.sh builds
// the distributions and runs the task integrationTest. The build of the module only compiles the tests.
//
// No API of the nodes publishes the authorized data of a security manager, such as an alert. The tests therefore start
// the program of the source set authorizedDataPublisher as a node of this checkout. Only this source set uses the code
// of Bisq, so the tests themselves stay limited to the API and the logs.
val authorizedDataPublisher: SourceSet by sourceSets.creating

dependencies {
    integrationTestImplementation(libs.jackson.databind)

    "authorizedDataPublisherImplementation"(project(":api-app"))
    "authorizedDataPublisherImplementation"("bisq:application")
    "authorizedDataPublisherImplementation"("bisq:bonded-roles")
    "authorizedDataPublisherImplementation"("bisq:common")
    "authorizedDataPublisherImplementation"("bisq:identity")
    "authorizedDataPublisherImplementation"("bisq:java-se")
    "authorizedDataPublisherImplementation"("bisq:persistence")
    "authorizedDataPublisherImplementation"("bisq:security")
    "authorizedDataPublisherImplementation"("bisq:user")
    "authorizedDataPublisherImplementation"("network:network")
    "authorizedDataPublisherImplementation"("network:network-identity")
    "authorizedDataPublisherImplementation"(libs.google.guava)
    "authorizedDataPublisherImplementation"(libs.slf4j.api)
}

val authorizedDataPublisherClasspath: FileCollection = authorizedDataPublisher.runtimeClasspath

tasks.named<Test>("integrationTest") {
    listOf("bisq.e2e.newTree", "bisq.e2e.oldTree", "bisq.e2e.oldVersion", "bisq.e2e.workDir", "bisq.e2e.libfaketime").forEach { name ->
        val value = providers.gradleProperty(name)
        inputs.property(name, value.orElse(""))
        systemProperty(name, value.orElse("").get())
    }
    // The tests start the authorized data publisher with this classpath, so Gradle builds it before the tests run
    inputs.files(authorizedDataPublisherClasspath)
        .withPropertyName("authorizedDataPublisherClasspath")
        .withNormalizer(ClasspathNormalizer::class)
    jvmArgumentProviders.add(CommandLineArgumentProvider {
        listOf("-Dbisq.e2e.authorizedDataPublisherClasspath=" + authorizedDataPublisherClasspath.asPath)
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

// The build compiles the tests and the authorized data publisher, so that the CI build finds compile errors, but it
// does not run the tests
tasks.named("assemble") {
    dependsOn("integrationTestClasses", "authorizedDataPublisherClasses")
}
