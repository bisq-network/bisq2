// Used by .github/workflows/codeql.yml. Gradle applies an init script to every build of the composite, so the root
// project of each build gets a task which compiles the main sources of all its projects without running tests or checks.
gradle.rootProject {
    tasks.register("compileMainSourcesForCodeQL") {
        dependsOn(allprojects.map { project -> project.tasks.matching { it.name == "classes" } })
    }
}
