package bisq.gradle.toolchain_resolver

import org.gradle.jvm.toolchain.JavaToolchainDownload
import org.gradle.jvm.toolchain.JavaToolchainRequest
import org.gradle.jvm.toolchain.JavaToolchainResolver
import org.gradle.platform.Architecture
import org.gradle.platform.OperatingSystem
import java.net.URI
import java.util.*

/**
 * Resolves the Corretto JDK of the requested platform. A JDK of another architecture does not run, so the resolver
 * returns nothing for a platform without a known build. Gradle then
 * reports that no toolchain can be provisioned, instead of installing a JDK which cannot start.
 *
 * The Java 21 builds must be at or above releaseBuild.javaVersion, the minimum which verifyBuildEnvironment requires.
 * Update them when that minimum is raised.
 */
@Suppress("UnstableApiUsage")
abstract class BisqToolchainResolver : JavaToolchainResolver {
    override fun resolve(toolchainRequest: JavaToolchainRequest): Optional<JavaToolchainDownload> {
        if (!toolchainRequest.javaToolchainSpec.vendor.get().matches("Amazon.com Inc.")) {
            return Optional.empty()
        }
        val operatingSystem = toolchainRequest.buildPlatform.operatingSystem
        val architecture = toolchainRequest.buildPlatform.architecture
        val javaVersion = toolchainRequest.javaToolchainSpec.languageVersion.get().asInt()
        val toolchainUrl: String = when (operatingSystem) {
            OperatingSystem.LINUX -> getToolchainUrlForLinux(javaVersion, architecture)
            OperatingSystem.MAC_OS -> getToolchainUrlForMacOs(javaVersion, architecture)
            OperatingSystem.WINDOWS -> getToolchainUrlForWindows(javaVersion, architecture)
            else -> null

        } ?: return Optional.empty()

        val uri = URI(toolchainUrl)
        return Optional.of(
                JavaToolchainDownload.fromUri(uri)
        )
    }

    private fun getToolchainUrlForLinux(javaVersion: Int, architecture: Architecture): String? =
            when (architecture) {
                Architecture.X86_64 -> when (javaVersion) {
                    21 -> "https://corretto.aws/downloads/resources/21.0.13.1.2/amazon-corretto-21.0.13.1.2-linux-x64.tar.gz"
                    else -> null
                }
                Architecture.AARCH64 -> when (javaVersion) {
                    21 -> "https://corretto.aws/downloads/resources/21.0.13.1.2/amazon-corretto-21.0.13.1.2-linux-aarch64.tar.gz"
                    else -> null
                }
                else -> null
            }

    private fun getToolchainUrlForMacOs(javaVersion: Int, architecture: Architecture): String? {
        val macOsArchName = getMacOsArchitectureClassifier(architecture) ?: return null
        return when (javaVersion) {
            21 -> "https://corretto.aws/downloads/resources/21.0.13.1.2/amazon-corretto-21.0.13.1.2-macosx-" + macOsArchName + ".tar.gz"
            else -> null
        }
    }

    private fun getToolchainUrlForWindows(javaVersion: Int, architecture: Architecture): String? =
            when (architecture) {
                Architecture.X86_64 -> when (javaVersion) {
                    21 -> "https://corretto.aws/downloads/resources/21.0.13.1.2/amazon-corretto-21.0.13.1.2-windows-x64-jdk.zip"
                    else -> null
                }
                else -> null
            }

    private fun getMacOsArchitectureClassifier(architecture: Architecture): String? =
            when (architecture) {
                Architecture.AARCH64 -> "aarch64"
                Architecture.X86_64 -> "x64"
                else -> null
            }
}
