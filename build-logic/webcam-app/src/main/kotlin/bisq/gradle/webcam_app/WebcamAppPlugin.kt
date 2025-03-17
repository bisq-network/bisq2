package bisq.gradle.webcam_app

import bisq.gradle.tasks.VersionUtil
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.register

class WebcamAppPlugin : Plugin<Project> {
    override fun apply(project: Project) {

        val copyWebcamAppVersionToDesktop = project.tasks.register<Copy>("copyWebcamAppVersionToDesktop") {
            val desktopProject = project.parent?.childProjects?.filter { e -> e.key == "desktop" }?.map { e -> e.value.project }?.first()
            desktopProject?.tasks?.let {
                from(project.layout.projectDirectory.asFile.absolutePath + "/version.txt")
                include("version.txt")
                into(desktopProject.layout.buildDirectory.dir("generated/src/main/resources/webcam-app"))
            }
        }

        val zipWebcamAppShadowJar: TaskProvider<Zip> = project.tasks.register<Zip>("zipWebcamAppShadowJar") {
            dependsOn(project.tasks.named("shadowJar"))

            val version = VersionUtil.getVersionFromFile(project)
            archiveFileName.set("webcam-app-$version.zip")
            destinationDirectory.set(project.layout.buildDirectory.dir("generated"))
            include("webcam-app-$version-all.jar")
            from(project.layout.buildDirectory.dir("libs"))
        }

        val copyWebcamAppVersionToResources = project.tasks.register<Copy>("copyWebcamAppVersionToResources") {
            dependsOn(project.tasks.named("processResources"))

            from(project.layout.projectDirectory.asFile.absolutePath + "/version.txt")
            into(project.layout.buildDirectory.dir("generated/src/main/resources/webcam-app"))
        }

        project.tasks.register<Copy>("processWebcamForDesktop") {
            dependsOn(copyWebcamAppVersionToResources)
            dependsOn(zipWebcamAppShadowJar)
            dependsOn(copyWebcamAppVersionToDesktop)

            val desktopProject = project.parent?.childProjects?.filter { e -> e.key == "desktop" }?.map { e -> e.value.project }?.first()
            desktopProject?.tasks?.let {
                from(project.layout.buildDirectory.dir("generated"))
                exclude("sources")
                into(desktopProject.layout.buildDirectory.dir("generated/src/main/resources/webcam-app"))
            }
        }
    }
}