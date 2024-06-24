package bisq.gradle.packaging

import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.register

class ReleaseBinariesTaskFactory(private val project: Project) {
    companion object {
        private const val MAINTAINER_PUBLIC_KEY_DIRECTORY: String = "maintainer_public_keys"
    }

    private val releaseDir: Provider<Directory> = project.layout.buildDirectory.dir("packaging/release")
    private val inputBinariesProperty: Provider<String> = project.providers
        .gradleProperty("bisq.release.binaries_path")

    fun registerCopyReleaseBinariesTask() {
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
        val maintainerPublicKeys = project.layout.files(
            "$MAINTAINER_PUBLIC_KEY_DIRECTORY/387C8307.asc",
            "$MAINTAINER_PUBLIC_KEY_DIRECTORY/E222AA02.asc"
        )
        project.tasks.register<Copy>("copyMaintainerPublicKeys") {
            from(maintainerPublicKeys)
            into(releaseDir)
        }
    }

    fun registerCopySigningPublicKeyTask() {
        val signingPublicKey = project.layout.projectDirectory
            .file("$MAINTAINER_PUBLIC_KEY_DIRECTORY/E222AA02.asc")
        project.tasks.register<Copy>("copySigningPublicKey") {
            from(signingPublicKey)
            into(releaseDir)
            rename { "signingkey.asc" }
        }
    }

    fun registerMergeOsSpecificJarHashesTask() {
        val files = project.files(
            inputBinariesProperty.map { inputDir ->
                "$inputDir/desktop-${PackagingPlugin.APP_VERSION}-all-mac.jar.SHA-256" },
            inputBinariesProperty.map { inputDir ->
                "$inputDir/desktop-${PackagingPlugin.APP_VERSION}-all-linux.jar.SHA-256" },
            inputBinariesProperty.map { inputDir ->
                "$inputDir/desktop-${PackagingPlugin.APP_VERSION}-all-windows.jar.SHA-256" }
        )
        val mergedShaFile: Provider<RegularFile> = project.layout.buildDirectory
            .file("packaging/release/Bisq-${PackagingPlugin.APP_VERSION}.jar.txt")
        project.tasks.register<MergeOsSpecificJarHashesTask>("mergeOsSpecificJarHashes") {
            hashFiles.setFrom(files)
            outputFile.set(mergedShaFile)
        }
    }
}