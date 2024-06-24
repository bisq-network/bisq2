package bisq.gradle.packaging

import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.register

class ReleaseBinariesTaskFactory(private val project: Project) {
    private val releaseDir: Provider<Directory> = project.layout.buildDirectory.dir("packaging/release")

    fun registerCopyReleaseBinariesTask() {
        val inputBinariesProperty: Provider<String> = project.providers
            .gradleProperty("bisq.release.binaries_path")
        val releaseDir: Provider<Directory> = project.layout.buildDirectory.dir("packaging/release")

        project.tasks.register<Copy>("copyReleaseBinaries") {
            from(inputBinariesProperty)
            into(releaseDir)
            rename { fileName: String ->
                fileName.replace("Bisq 2", "Bisq") // "Bisq 2-2.0.4.exe", "Bisq 2-2.0.4.dmg"
                    .replace("bisq2_", "Bisq-") // "bisq2_2.0.4-1_amd64.deb"
                    .replace("bisq2-", "Bisq-") // "bisq2-2.0.4-1.x86_64.rpm"
                    .replace("-1_amd64", "")
                    .replace("-1.x86_64", "")
            }
        }
    }

    fun registerCopyMaintainerPublicKeysTask() {
        val publicKeyDirectory = "maintainer_public_keys"
        val maintainerPublicKeys = project.layout.files(
            "$publicKeyDirectory/387C8307.asc",
            "$publicKeyDirectory/E222AA02.asc"
        )
        project.tasks.register<Copy>("copyMaintainerPublicKeys") {
            from(maintainerPublicKeys)
            into(releaseDir)
        }
    }
}