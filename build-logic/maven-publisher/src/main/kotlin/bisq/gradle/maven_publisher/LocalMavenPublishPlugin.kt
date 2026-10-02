package bisq.gradle.maven_publisher

import groovy.util.Node
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.GenerateMavenPom
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
        val COMPOSITE_PROJECTS_TO_INCLUDE = listOf("tor", "network")
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
                val generateProtoTask = project.tasks.findByName("generateProto")
                val protoSourcesJar = project.tasks.findByName("protoSourcesJar") ?: project.tasks.register("protoSourcesJar", Jar::class.java) {
                    archiveClassifier.set("proto-sources")
                    val protoDir = project.layout.buildDirectory.dir("generated/source/proto/main").get().asFile
                    from(project.fileTree(protoDir))
                    if (generateProtoTask != null) {
                        dependsOn(generateProtoTask)
                    }
                }

                if (rootVersion == "unspecified") {
                    throw IllegalStateException("Root project version not set. Please set the rootVersion property.")
                }
                project.extensions.configure<PublishingExtension>("publishing") {
                    publications {
//                        val publicationName = if (group == DEFAULT_GROUP) "mavenJava" else "mavenJava_${group}"
                        val publicationName = "mavenJava"
                        val existingPublication = findByName(publicationName) ?: create(publicationName, MavenPublication::class)
                        (existingPublication as MavenPublication).apply {
                            from(project.components["java"])  // Adjust if publishing other types (like Kotlin)
                            artifactId = project.name
                            groupId = group
                            version = rootVersion

                            setupPublication(protoSourcesJar)
                        }
                    }
                    repositories {
                        maven {
                            name = "local"
                            url = uri("${System.getProperty("user.home")}/.m2/repository")
                        }
                    }
                }

                // Runs the POM checks, like requireVersionsOnOwnDependencies, in every build and in CI, which never
                // publishes
                project.tasks.named("check") {
                    dependsOn(project.tasks.withType(GenerateMavenPom::class.java))
                }
            } else {
                println("${project.name} does not have a Java component, skipping")
            }
        }
    }

    private fun MavenPublication.setupPublication(protoSourcesJar: Any) {
        // Include the Protobuf sources JAR
        artifact(protoSourcesJar)

        pom.withXml {
            val rootNode = asNode()
            removeOwnBomImports(rootNode)
            requireVersionsOnOwnDependencies(rootNode)
        }
    }

    // Gradle consumers get bisq:platform from the Gradle module metadata. The POM import of it cannot be resolved on
    // JitPack, which moves our dependencies to its own coordinates but not BOM imports, and Gradle resolves the import
    // while it reads the POM, before it switches to the module metadata.
    private fun removeOwnBomImports(rootNode: Node) {
        val dependencyManagementNode = rootNode.childNode("dependencyManagement") ?: return
        val dependenciesNode = dependencyManagementNode.childNode("dependencies") ?: return
        dependenciesNode.childNodes()
            .filter { it.isOwnDependency() && it.childNode("scope")?.text() == "import" }
            .forEach { dependenciesNode.remove(it) }
        if (dependenciesNode.childNodes().isEmpty()) {
            rootNode.remove(dependencyManagementNode)
        }
    }

    // A dependency like implementation("bisq:common") still builds, because the composite build substitutes the
    // project, but it is published without a version, and projects using the published jars cannot resolve it. The same
    // goes for "bisq:common:$version" in a project whose own version is not set, which is published as "unspecified".
    private fun MavenPublication.requireVersionsOnOwnDependencies(rootNode: Node) {
        val withoutVersion = rootNode.childNode("dependencies")?.childNodes().orEmpty()
            .filter { it.isOwnDependency() && it.childNode("version")?.text() in listOf(null, "unspecified") }
            .map { dependency ->
                listOf("groupId", "artifactId", "version")
                    .mapNotNull { dependency.childNode(it)?.text() }
                    .joinToString(":")
            }
        if (withoutVersion.isNotEmpty()) {
            throw GradleException("$groupId:$artifactId declares $withoutVersion without a version. " +
                    "Add one, for example implementation(\"bisq:common:\$version\"), in a project that sets its version.")
        }
    }

    private fun Node.isOwnDependency(): Boolean {
        val groupId = childNode("groupId")?.text() ?: return false
        return groupId == DEFAULT_GROUP || groupId in COMPOSITE_PROJECTS_TO_INCLUDE
    }

    private fun Node.childNodes(): List<Node> = children().filterIsInstance<Node>()

    // Names carry the POM namespace, hence the suffix match
    private fun Node.childNode(name: String): Node? = childNodes().firstOrNull { it.name().toString().endsWith(name) }

    private fun loadRootVersion(project: Project) {        val rootPropertiesFile = File(getRootGradlePropertiesFile(project), "gradle.properties")
        if (project.version != "unspecified") {
            rootVersion = project.version as String
        } else {
            val rootProperties = Properties()
            if (rootPropertiesFile.exists()) {
                rootProperties.load(rootPropertiesFile.inputStream())
            }

            rootVersion = rootProperties.getProperty("version", "unspecified")
        }
    }

    private fun getRootGradlePropertiesFile(project: Project): File {
        if (COMPOSITE_PROJECTS_TO_INCLUDE.contains(project.name) && project.childProjects.isNotEmpty()) {
            return when (project.name) {
                "tor" -> project.projectDir.parentFile.parentFile
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