package bisq.gradle.webcam_app

import bisq.gradle.common.VersionUtil
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.register

class WebcamAppPlugin : Plugin<Project> {
    override fun apply(project: Project) {

        val zipWebcamAppShadowJar: TaskProvider<Zip> = project.tasks.register<Zip>("zipWebcamAppShadowJar") {
            dependsOn(project.tasks.named("shadowJar"))

            val version = VersionUtil.getVersionFromFile(project)
            archiveFileName.set("webcam-app-$version.zip")
            destinationDirectory.set(project.layout.buildDirectory.dir("generated"))
            include("webcam-app-$version-all.jar")
            from(project.layout.buildDirectory.dir("libs"))
            isPreserveFileTimestamps = false
            isReproducibleFileOrder = true
            // The single entry is the shadow jar, whose on-disk mode follows the umask of the building machine.
            filePermissions { unix("0644") }
        }

        val copyWebcamAppVersionToResources = project.tasks.register<Copy>("copyWebcamAppVersionToResources") {
            dependsOn(project.tasks.named("processResources"))

            from(project.layout.projectDirectory.asFile.absolutePath + "/version.txt")
            into(project.layout.buildDirectory.dir("generated/src/main/resources/webcam-app"))
        }

        project.tasks.register<Sync>("processWebcamForDesktop") {
            dependsOn(copyWebcamAppVersionToResources)
            dependsOn(zipWebcamAppShadowJar)

            val desktopProject = project.parent?.childProjects?.filter { e -> e.key == "desktop" }?.map { e -> e.value.project }?.first()
            desktopProject?.tasks?.let {
                from(project.layout.buildDirectory.dir("generated")) {
                    exclude("sources")
                    include("webcam-app-" + VersionUtil.getVersionFromFile(project) + ".zip")
                    include("src/**")
                }
                from(project.layout.projectDirectory.file("version.txt"))
                into(desktopProject.layout.buildDirectory.dir("generated/src/main/resources/webcam-app"))
            }
        }
    }
}