package bisq.gradle.maven_publisher

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*
import java.io.File
import java.util.*

/**
 * This custom plugin configures the task publishToMavenLocal in the applied project.
 * If the project is a leaf, it directly setups the task along with all the maven publish plugin tasks.
 * otherwise, it will setup the task as well but just as a connector to the tasks of its child projects.
 * This plugin plays along well with the root custom task called "publishAll"
 */
class LocalMavenPublishPlugin : Plugin<Project> {
    companion object {
        const val GROUP_SEPARATOR = "."
        val COMPOSITE_PROJECTS_TO_INCLUDE = listOf("tor")
    }

    private var rootVersion = "unspecified"

    override fun apply(project: Project) {
        this.loadRootVersion(project)
        applyTaskRecursively(project, getRootGroup(project))
    }

    private fun applyTaskRecursively(project: Project, group: String) {
        println("Configuring project ${project.name}")
        project.plugins.apply("maven-publish")

        project.afterEvaluate {
            if (project.subprojects.isEmpty()) {
                // If it's a leaf project, proceed with publishing
                applyPublishPlugin(project, group)
            } else {
                project.subprojects {
                    applyTaskRecursively(this, "${group}${GROUP_SEPARATOR}${project.name}")
                }

                val existingTask = project.tasks.findByName("publishToMavenLocal")
                // Create a new task that publishes subprojects
                if (existingTask == null) {
                    project.tasks.register("publishToMavenLocal") {
                        dependsOn(project.subprojects.map { it.tasks.getByName("publishToMavenLocal") })
                    }
                } else {
                    project.tasks.named("publishToMavenLocal").configure {
                        dependsOn(project.subprojects.map { it.tasks.getByName("publishToMavenLocal") })
                    }
                }
            }
        }
    }

    private fun applyPublishPlugin(project: Project, group: String) {
        project.afterEvaluate {
            val javaComponent = project.components.findByName("java")
            if (javaComponent != null) {
                val protoSourcesJar = project.tasks.register("protoSourcesJar", Jar::class.java) {
                    archiveClassifier.set("proto-sources")
                    from(project.fileTree("${project.layout.buildDirectory}/generated/source/proto/main"))  // Adjust path if needed
                }
                project.extensions.configure<PublishingExtension>("publishing") {
                    publications {
                        create("mavenJava", MavenPublication::class) {
                            from(project.components["java"])  // Adjust if publishing other types (like Kotlin)
                            artifactId = project.name
                            groupId = group
                            version = rootVersion

                            // Include the Protobuf sources JAR
                            artifact(protoSourcesJar)
                        }
                    }
                    repositories {
                        maven {
                            name = "local"
                            url = uri("${System.getProperty("user.home")}/.m2/repository")
                        }
                    }
                }
            } else {
                println("${project.name} does not have a Java component, skipping")
            }
        }
    }

    private fun loadRootVersion(project: Project) {
        // Load properties from root gradle.properties file
        val rootPropertiesFile = File(getRootGradlePropertiesFile(project), "gradle.properties")
        val rootProperties = Properties()
        if (rootPropertiesFile.exists()) {
            rootProperties.load(rootPropertiesFile.inputStream())
        }

        rootVersion = rootProperties.getProperty("version", "unspecified")
    }

    private fun getRootGradlePropertiesFile(project: Project): File {
        if (COMPOSITE_PROJECTS_TO_INCLUDE.contains(project.name)) {
            return project.projectDir.parentFile.parentFile
        }
        return project.projectDir.parentFile
    }

    private fun getRootGroup(project: Project): String {
        if (COMPOSITE_PROJECTS_TO_INCLUDE.contains(project.name)) {
            return "bisq.network"
        }
        return "bisq"
    }
}