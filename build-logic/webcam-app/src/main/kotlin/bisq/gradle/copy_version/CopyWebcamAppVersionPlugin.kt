package bisq.gradle.copy_version

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.register

class CopyWebcamAppVersionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register<Copy>("copyWebcamAppVersion") {
            val desktopProject = project.parent?.childProjects?.filter { e -> e.key == "desktop" }?.map { e -> e.value.project }?.first()
            val webcamProject = project.parent?.childProjects?.filter { e -> e.key == "webcam" }?.map { e -> e.value.project }?.first()
            desktopProject?.tasks?.let {
                webcamProject?.tasks?.let {
                    from(webcamProject.layout.projectDirectory.asFile.absolutePath + "/version.txt")
                    into(desktopProject.layout.buildDirectory.dir("generated/src/main/resources/webcam-app"))
                }
            }
        }
    }
}