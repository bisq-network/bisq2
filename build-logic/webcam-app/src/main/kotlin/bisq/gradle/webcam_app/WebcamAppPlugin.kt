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
            archiveFileName.set("webcam-$version.zip")
            destinationDirectory.set(project.layout.buildDirectory.dir("generated"))
            include("webcam-$version-all.jar")
            from(project.layout.buildDirectory.dir("libs"))
        }

        project.tasks.register<Copy>("processWebcamForDesktop") {
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