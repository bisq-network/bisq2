package bisq.gradle.bi2p

import bisq.gradle.common.VersionUtil
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.language.jvm.tasks.ProcessResources

class Bi2pAppPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val zipBi2pAppShadowJar: TaskProvider<Zip> = project.tasks.register<Zip>("zipBi2pAppShadowJar") {
            dependsOn(project.tasks.named("shadowJar"))

            val version = VersionUtil.getVersionFromFile(project)
            archiveFileName.set("bi2p-$version.zip")
            destinationDirectory.set(project.layout.buildDirectory.dir("generated"))
            include("bi2p-$version-all.jar")
            from(project.layout.buildDirectory.dir("libs"))
            isPreserveFileTimestamps = false
            isReproducibleFileOrder = true
            // The single entry is the shadow jar, whose on-disk mode follows the umask of the building machine.
            filePermissions { unix("0644") }
        }

        project.pluginManager.withPlugin("java") {
            project.tasks.named<ProcessResources>("processResources") {
                from(project.layout.projectDirectory.file("version.txt")) {
                    into("bi2p")
                }
            }
        }

        project.tasks.register<Sync>("processBi2pForDesktop") {
            dependsOn(zipBi2pAppShadowJar)

            val desktopProject = project.rootProject.findProject(":apps:desktop")
                ?: project.rootProject.subprojects.find { it.name == "desktop" }
            requireNotNull(desktopProject) { "Desktop project not found (expected ':apps:desktop')." }
            from(project.layout.buildDirectory.dir("generated")) {
                include("bi2p-" + VersionUtil.getVersionFromFile(project) + ".zip")
            }
            from(project.layout.projectDirectory.file("version.txt"))
            into(desktopProject.layout.buildDirectory.dir("generated/src/main/resources/bi2p"))
        }
    }
}