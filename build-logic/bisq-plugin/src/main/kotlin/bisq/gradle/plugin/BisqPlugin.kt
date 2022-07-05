package bisq.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project


class BisqPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply("bisq.java-library")
    }
}