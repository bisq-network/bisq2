package bisq.gradle.i2p_router

import bisq.gradle.common.VersionUtil
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.register

class I2pRouterAppPlugin : Plugin<Project> {
    override fun apply(project: Project) {

        val copyI2pRouterAppVersionToDesktop = project.tasks.register<Copy>("copyI2pRouterAppVersionToDesktop") {
            val desktopProject = project.parent?.childProjects?.filter { e -> e.key == "desktop" }?.map { e -> e.value.project }?.first()
            desktopProject?.tasks?.let {
                from(project.layout.projectDirectory.asFile.absolutePath + "/version.txt")
                include("version.txt")
                into(desktopProject.layout.buildDirectory.dir("generated/src/main/resources/i2p-router-app"))
            }
        }

        val zipI2pRouterAppShadowJar: TaskProvider<Zip> = project.tasks.register<Zip>("zipI2pRouterAppShadowJar") {
            dependsOn(project.tasks.named("shadowJar"))

            val version = VersionUtil.getVersionFromFile(project)
            archiveFileName.set("i2p-router-app-$version.zip")
            destinationDirectory.set(project.layout.buildDirectory.dir("generated"))
            include("i2p-router-app-$version-all.jar")
            from(project.layout.buildDirectory.dir("libs"))
        }

        val copyI2pRouterAppVersionToResources = project.tasks.register<Copy>("copyI2pRouterAppVersionToResources") {
            dependsOn(project.tasks.named("processResources"))

            from(project.layout.projectDirectory.asFile.absolutePath + "/version.txt")
            into(project.layout.buildDirectory.dir("generated/src/main/resources/i2p-router-app"))
        }

        project.tasks.register<Copy>("processI2pRouterForDesktop") {
            dependsOn(copyI2pRouterAppVersionToResources)
            dependsOn(zipI2pRouterAppShadowJar)
            dependsOn(copyI2pRouterAppVersionToDesktop)

            val desktopProject = project.parent?.childProjects?.filter { e -> e.key == "desktop" }?.map { e -> e.value.project }?.first()
            desktopProject?.tasks?.let {
                from(project.layout.buildDirectory.dir("generated"))
                exclude("sources")
                into(desktopProject.layout.buildDirectory.dir("generated/src/main/resources/i2p-router-app"))
            }
        }
    }
}