package bisq.gradle.webcam_app

import bisq.gradle.tasks.VersionUtil
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.register

class WebcamAppPlugin : Plugin<Project> {
    override fun apply(project: Project) {

        project.tasks.register<Zip>("zipWebcamAppShadowJar") {
            dependsOn(project.tasks.named("shadowJar"))

            val version = VersionUtil.getVersionFromFile(project)
            archiveFileName.set("webcam-$version.zip")
            destinationDirectory.set(project.layout.buildDirectory.dir("generated"))
            from(project.layout.buildDirectory.dir("libs"))
            include("webcam-$version-all.jar")
        }
    }
}