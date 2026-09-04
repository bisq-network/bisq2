plugins {
    id("bisq.java-library")
    id("bisq.protobuf")
}

// PersistableStoreFinalityTest scans the java sources of every module, including the separate `apps` and `network`
// builds, so those sources have to be declared as inputs. Without this the task stays up-to-date and the test silently
// does not run when a store in another module changes.
tasks.test {
    inputs.files(
        fileTree(rootDir) {
            include("**/src/main/java/**/*.java")
            exclude("**/build/**", "**/.git/**", "**/.gradle/**")
        }
    ).withPropertyName("moduleJavaSources").withPathSensitivity(PathSensitivity.RELATIVE)
}
