package bisq.gradle.bi2p

import bisq.gradle.common.VersionUtil
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.register

class Bi2pAppPlugin : Plugin<Project> {
    override fun apply(project: Project) {

        val copyBi2pAppVersionToDesktop = project.tasks.register<Copy>("copyBi2pAppVersionToDesktop") {
            val desktopProject = project.parent?.childProjects?.filter { e -> e.key == "desktop" }?.map { e -> e.value.project }?.first()
            desktopProject?.tasks?.let {
                from(project.layout.projectDirectory.asFile.absolutePath + "/version.txt")
                include("version.txt")
                into(desktopProject.layout.buildDirectory.dir("generated/src/main/resources/bi2p"))
            }
        }

        val zipBi2pAppShadowJar: TaskProvider<Zip> = project.tasks.register<Zip>("zipBi2pAppShadowJar") {
            dependsOn(project.tasks.named("shadowJar"))

            val version = VersionUtil.getVersionFromFile(project)
            archiveFileName.set("bi2p-$version.zip")
            destinationDirectory.set(project.layout.buildDirectory.dir("generated"))
            include("bi2p-$version-all.jar")
            from(project.layout.buildDirectory.dir("libs"))
        }

        val copyBi2pAppVersionToResources = project.tasks.register<Copy>("copyBi2pAppVersionToResources") {
            dependsOn(project.tasks.named("processResources"))

            from(project.layout.projectDirectory.asFile.absolutePath + "/version.txt")
            into(project.layout.buildDirectory.dir("generated/src/main/resources/bi2p"))
        }

        project.tasks.register<Copy>("processBi2pForDesktop") {
            dependsOn(copyBi2pAppVersionToResources)
            dependsOn(zipBi2pAppShadowJar)
            dependsOn(copyBi2pAppVersionToDesktop)

            val desktopProject = project.parent?.childProjects?.filter { e -> e.key == "desktop" }?.map { e -> e.value.project }?.first()
            desktopProject?.tasks?.let {
                from(project.layout.buildDirectory.dir("generated"))
                exclude("sources")
                into(desktopProject.layout.buildDirectory.dir("generated/src/main/resources/bi2p"))
            }
        }
    }
}