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
    private val defaultInputDir: Provider<Directory> = project.layout.buildDirectory.dir("packaging/jpackage/packages")

    fun registerCopyReleaseBinariesTask() {
        val releaseDir: Provider<Directory> = project.layout.buildDirectory.dir("packaging/release")
        project.tasks.register<Copy>("copyReleaseBinaries") {
            if (inputBinariesProperty.isPresent) {
                from(inputBinariesProperty)
            } else {
                from(defaultInputDir)
            }
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
                // For Bisq 2 we do not use Bisq2 but the version number contains the '2'
                val canonicalFileName = fileName.replace("bisq", "Bisq")
                        .replace("Bisq2", "Bisq")
                        .replace("Bisq_", "Bisq-")
                        .replace("-1_amd64", "")
                        .replace("-1.x86_64", "")
                val fileWithoutExtension = canonicalFileName.substring(0, canonicalFileName.length - 4)
                val fileExtension = canonicalFileName.substring(canonicalFileName.length - 4, canonicalFileName.length)
                val platformName = getPlatform().platformName
                // E.g. Bisq-2.0.4-macos_arm64.dmg
                "$fileWithoutExtension-$platformName$fileExtension"
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
        // E.g. For Bisq-2.0.4-macos_arm64.dmg -> Bisq-2.0.4-macos_arm64-all-jars.sha256
        val postFix = "-all-jars.sha256"
        val files = project.files(
                inputBinariesProperty.map { inputDir ->
                    appVersion.map { version -> "$inputDir/Bisq-$version-${Platform.LINUX_X86_64.platformName}$postFix" }
                },
                inputBinariesProperty.map { inputDir ->
                    appVersion.map { version -> "$inputDir/Bisq-$version-${Platform.LINUX_ARM_64.platformName}$postFix" }
                },
                inputBinariesProperty.map { inputDir ->
                    appVersion.map { version -> "$inputDir/Bisq-$version-${Platform.MACOS_X86_64.platformName}$postFix" }
                },
                inputBinariesProperty.map { inputDir ->
                    appVersion.map { version -> "$inputDir/Bisq-$version-${Platform.MACOS_ARM_64.platformName}$postFix" }
                },
                inputBinariesProperty.map { inputDir ->
                    appVersion.map { version -> "$inputDir/Bisq-$version-${Platform.WIN_X86_64.platformName}$postFix" }
                }
        )

        val mergedShaFile: Provider<RegularFile> = appVersion.flatMap { version ->
            project.layout.buildDirectory.file("packaging/release/Bisq-$version$postFix")
        }

        project.tasks.register<MergeOsSpecificJarHashesTask>("mergeOsSpecificJarHashes") {
            hashFiles.setFrom(files)
            outputFile.set(mergedShaFile)
        }
    }
}