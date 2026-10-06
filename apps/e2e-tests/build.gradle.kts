plugins {
    id("bisq.java-library")
    id("bisq.java-integration-tests")
}

// Black-box tests of networks with nodes of two versions. The tests start the api-app and seed node distributions of
// both versions as processes and use only the API and the logs of the nodes. tools/compat/run-mixed-network.sh builds
// the distributions and runs the task integrationTest. The build of the module only compiles the tests.
dependencies {
    integrationTestImplementation(libs.jackson.databind)
}

tasks.named<Test>("integrationTest") {
    listOf("bisq.e2e.newTree", "bisq.e2e.oldTree", "bisq.e2e.oldVersion", "bisq.e2e.workDir", "bisq.e2e.libfaketime").forEach { name ->
        val value = providers.gradleProperty(name)
        inputs.property(name, value.orElse(""))
        systemProperty(name, value.orElse("").get())
    }
    // The result depends on processes and files outside of Gradle
    outputs.upToDateWhen { false }
    // Each test class starts its own network. Several networks at the same time would need too much memory.
    maxParallelForks = 1
    testLogging {
        events("passed", "skipped", "failed", "standardError")
        showStandardStreams = false
    }
}

// The build compiles the tests, so that the CI build finds compile errors, but it does not run them
tasks.named("assemble") {
    dependsOn("integrationTestClasses")
}
