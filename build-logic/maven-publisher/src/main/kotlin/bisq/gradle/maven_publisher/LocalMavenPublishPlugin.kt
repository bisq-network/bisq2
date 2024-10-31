package bisq.gradle.maven_publisher

import groovy.util.Node
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
        const val DEFAULT_GROUP = "bisq"
        val COMPOSITE_PROJECTS_TO_INCLUDE = listOf("tor", "network", "wallets", "bitcoind")
    }

    private var rootVersion = "unspecified"

    override fun apply(project: Project) {
        if (rootVersion == "unspecified") {
            this.loadRootVersion(project)
        }
        val group = getRootGroup(project);
        applyTaskRecursively(project, group)
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
                    applyTaskRecursively(this, group)
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
                val protoSourcesJar = project.tasks.findByName("protoSourcesJar") ?: project.tasks.register("protoSourcesJar", Jar::class.java) {
                    archiveClassifier.set("proto-sources")
                    from(project.fileTree("${project.layout.buildDirectory}/generated/source/proto/main"))  // Adjust path if needed
                }

                if (rootVersion == "unspecified") {
                    throw IllegalStateException("Root project version not set. Please set the rootVersion property.")
                }
                project.extensions.configure<PublishingExtension>("publishing") {
                    publications {
//                        val publicationName = if (group == DEFAULT_GROUP) "mavenJava" else "mavenJava_${group}"
                        val publicationName = "mavenJava"
                        create(publicationName, MavenPublication::class) {
                            from(project.components["java"])  // Adjust if publishing other types (like Kotlin)
                            artifactId = project.name
                            groupId = group
                            version = rootVersion

                            setupPublication(project, group, protoSourcesJar)
                        }
                        if (group != DEFAULT_GROUP) {
                            create("mavenJava_bisqAlias", MavenPublication::class) {
                                groupId = "bisq"
                                artifactId = project.name
                                version = rootVersion

                                setupPublication(project, group, protoSourcesJar, true)
                            }
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

    private fun MavenPublication.setupPublication(project: Project, group: String, protoSourcesJar: Any, isAlias: Boolean = false) {
        // Include the Protobuf sources JAR
        artifact(protoSourcesJar)
        // Reference the primary artifact and files
        if (isAlias) {
            artifact(project.tasks.named("jar"))
        }

        // hack to make sure the pom generated is compliant (without this it generates dependencies without the version)
        pom.withXml {
            val rootNode = asNode()
            if (isAlias) {
                rootNode.appendNode("description", "Alias of $group:${project.name}")
            }

            // Get all nodes with a name ending in "dependencies"
            val dependenciesNodes = rootNode.children().filter {
                (it as? Node)?.name().toString().endsWith("dependencies")
            }.map { it as Node }

            // fixes corrupted pom not resolving dependencies when not explicitly specified in gradle configuration
            val dependenciesNode = dependenciesNodes.firstOrNull() ?: rootNode.appendNode("dependencies")
            dependenciesNode.children().forEach { dependencyNode ->
                if (dependencyNode is Node) {
                    val versionNodes = dependencyNode.children().filter {
                        (it as? Node)?.name().toString().endsWith("version")
                    }.map { it as Node }
                    if (versionNodes.isEmpty()) {
                        dependencyNode.appendNode("version", rootVersion)
                    }
//                    else if (versionNodes[0].value() == null || versionNodes[0].value().toString() == "unspecified") {
//                        throw Error("${versionNodes[0]}")
//                    }
                }
            }
        }
    }

    private fun loadRootVersion(project: Project) {        val rootPropertiesFile = File(getRootGradlePropertiesFile(project), "gradle.properties")
        val rootProperties = Properties()
        if (rootPropertiesFile.exists()) {
            rootProperties.load(rootPropertiesFile.inputStream())
        }

        rootVersion = rootProperties.getProperty("version", "unspecified")
    }

    private fun getRootGradlePropertiesFile(project: Project): File {
        if (COMPOSITE_PROJECTS_TO_INCLUDE.contains(project.name) && project.childProjects.isNotEmpty()) {
            return when (project.name) {
                "tor", "bitcoind" -> project.projectDir.parentFile.parentFile
                else -> project.projectDir.parentFile
            }
        }
        return project.projectDir.parentFile
    }

    private fun getRootGroup(project: Project): String {
        if (COMPOSITE_PROJECTS_TO_INCLUDE.contains(project.name)) {
            return project.name
        }
        return DEFAULT_GROUP
    }
}