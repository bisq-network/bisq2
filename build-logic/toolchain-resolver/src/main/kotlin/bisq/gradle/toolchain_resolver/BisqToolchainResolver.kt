package bisq.gradle.toolchain_resolver

import org.gradle.jvm.toolchain.JavaToolchainDownload
import org.gradle.jvm.toolchain.JavaToolchainRequest
import org.gradle.jvm.toolchain.JavaToolchainResolver
import org.gradle.platform.Architecture
import org.gradle.platform.OperatingSystem
import java.net.URI
import java.util.*

/**
 * Resolves the Zulu JDK of the requested platform. Azul builds one JDK per architecture, and a JDK of another
 * architecture does not run, so the resolver returns nothing for a platform without a known build. Gradle then
 * reports that no toolchain can be provisioned, instead of installing a JDK which cannot start.
 */
@Suppress("UnstableApiUsage")
abstract class BisqToolchainResolver : JavaToolchainResolver {
    override fun resolve(toolchainRequest: JavaToolchainRequest): Optional<JavaToolchainDownload> {
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
                    11 -> "https://cdn.azul.com/zulu/bin/zulu11.66.15-ca-jdk11.0.20-linux_x64.zip"
                    17 -> "https://cdn.azul.com/zulu/bin/zulu17.44.15-ca-jdk17.0.8-linux_x64.zip"
                    21 -> "https://cdn.azul.com/zulu/bin/zulu21.54.107-ca-jdk21.0.13-linux_x64.zip"
                    else -> null
                }
                Architecture.AARCH64 -> when (javaVersion) {
                    21 -> "https://cdn.azul.com/zulu/bin/zulu21.54.107-ca-jdk21.0.13-linux_aarch64.tar.gz"
                    else -> null
                }
                else -> null
            }

    private fun getToolchainUrlForMacOs(javaVersion: Int, architecture: Architecture): String? {
        val macOsArchName = getMacOsArchitectureClassifier(architecture) ?: return null
        return when (javaVersion) {
            11 -> "https://cdn.azul.com/zulu/bin/zulu11.66.15_1-ca-jdk11.0.20-macosx_" + macOsArchName + ".tar.gz"
            15 -> "https://cdn.azul.com/zulu/bin/zulu15.46.17-ca-jdk15.0.10-macosx_" + macOsArchName + ".tar.gz"
            17 -> "https://cdn.azul.com/zulu/bin/zulu17.44.15_1-ca-jdk17.0.8-macosx_" + macOsArchName + ".tar.gz"
            21 -> "https://cdn.azul.com/zulu/bin/zulu21.54.107-ca-jdk21.0.13-macosx_" + macOsArchName + ".tar.gz"
            else -> null
        }
    }

    private fun getToolchainUrlForWindows(javaVersion: Int, architecture: Architecture): String? =
            when (architecture) {
                Architecture.X86_64 -> when (javaVersion) {
                    11 -> "https://cdn.azul.com/zulu/bin/zulu11.66.15-ca-jdk11.0.20-win_x64.zip"
                    17 -> "https://cdn.azul.com/zulu/bin/zulu17.44.15-ca-jdk17.0.8-win_x64.zip"
                    21 -> "https://cdn.azul.com/zulu/bin/zulu21.54.107-ca-jdk21.0.13-win_x64.zip"
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
