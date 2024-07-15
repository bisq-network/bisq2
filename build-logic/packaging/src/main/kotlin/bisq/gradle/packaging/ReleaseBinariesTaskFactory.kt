package bisq.gradle.packaging

import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
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

    // TODO we need to handle the mac binaries for aarch64 and x86 to be deployed as separate dmg files
    fun registerCopyReleaseBinariesTask() {
        val releaseDir: Provider<Directory> = project.layout.buildDirectory.dir("packaging/release")
        project.tasks.register<Copy>("copyReleaseBinaries") {
            from(inputBinariesProperty)
            include("*.dmg", "*.deb", "*.exe", "*.rpm")
            into(releaseDir)
            /* Bisq 1: "Bisq-1.9.15.dmg"          -> "Bisq-1.9.15.dmg"
                       "bisq_1.9.15-1_amd64.deb"  -> "Bisq-64bit-1.9.15.deb"
                       "Bisq-1.9.15.exe"          -> "Bisq-64bit-1.9.15.exe"
                       "bisq-1.9.15-1.x86_64.rpm" -> "Bisq-64bit-1.9.15.rpm"

               Bisq 2: "bisq2_2.0.4-1_amd64.deb"  -> "Bisq-2.0.4.deb"
                       "Bisq2-2.0.4.dmg"         -> "Bisq-2.0.4.dmg"
                       "Bisq2-2.0.4.exe"         -> "Bisq-2.0.4.exe"
                       "bisq2-2.0.4-1.x86_64.rpm" -> "Bisq-2.0.4.rpm" */
            rename { fileName: String ->
                if (fileName.startsWith("Bisq2") || fileName.contains("bisq2")) {
                    fileName.replace("Bisq2", "Bisq") // "Bisq2-2.0.4.exe", "Bisq2-2.0.4.dmg"
                        .replace("bisq2_", "Bisq-") // "bisq2_2.0.4-1_amd64.deb"
                        .replace("bisq2-", "Bisq-") // "bisq2-2.0.4-1.x86_64.rpm"
                        .replace("-1_amd64", "")
                        .replace("-1.x86_64", "")
                } else {
                    if (fileName.endsWith(".exe")) { // "Bisq-64bit-1.9.15.exe"
                        fileName.replace("Bisq-", "Bisq-64bit-")
                    } else if (fileName.endsWith(".rpm")) { //  Bisq-64bit-1.9.15.rpm
                        fileName.replace("bisq-", "Bisq-64bit-")
                            .replace("-1.x86_64.rpm", ".rpm")// "bisq-1.9.15-1.x86_64.rpm"
                    } else if (fileName.endsWith(".deb")) { // "bisq_1.9.15-1_amd64.deb"
                        fileName.replace("bisq_", "Bisq-64bit-")
                            .replace("-1_amd64.deb", ".deb")//  "Bisq-64bit-1.9.15.deb"
                    } else {
                        fileName
                    }
                }
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

    fun registerMergeOsSpecificJarHashesTask(appVersion: Property<String>) {
        val files = project.files(
            inputBinariesProperty.map { inputDir ->
                appVersion.map { "$inputDir/desktop-$it-all-mac.jar.SHA-256" }
            },
            inputBinariesProperty.map { inputDir ->
                appVersion.map { "$inputDir/desktop-$it-all-linux.jar.SHA-256" }
            },
            inputBinariesProperty.map { inputDir ->
                appVersion.map { "$inputDir/desktop-$it-all-windows.jar.SHA-256" }
            }
        )

        val mergedShaFile: Provider<RegularFile> = appVersion.flatMap {
            project.layout.buildDirectory.file("packaging/release/Bisq-$it.jar.txt")
        }

        project.tasks.register<MergeOsSpecificJarHashesTask>("mergeOsSpecificJarHashes") {
            hashFiles.setFrom(files)
            outputFile.set(mergedShaFile)
        }
    }
}