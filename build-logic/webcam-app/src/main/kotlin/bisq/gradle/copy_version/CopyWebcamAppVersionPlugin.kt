package bisq.gradle.copy_version

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.register

class CopyWebcamAppVersionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register<Copy>("copyWebcamAppVersion") {
            val webcamProject = project.parent?.childProjects?.filter { e -> e.key == "webcam" }?.map { e -> e.value.project }?.first()
            webcamProject?.tasks?.let {
                from(webcamProject.layout.projectDirectory.asFile.absolutePath + "/version.txt")
                into(project.layout.buildDirectory.dir("generated/src/main/resources/webcam-app"))
            }
        }
    }
}