package bisq.gradle.packaging.jpackage.package_formats

import java.nio.file.Files
import java.nio.file.Path
import java.util.*

class LinuxPackages(
        private val resourcesPath: Path,
        private val appName: String,
        private val appContentPaths: List<Path> = emptyList()
) : JPackagePackageFormatConfigs {
    override val packageFormats = setOf(PackageFormat.DEB, PackageFormat.RPM)

    override fun createArgumentsForJPackage(packageFormat: PackageFormat): List<String> {
        val arguments = mutableListOf(
                "--icon",
                resourcesPath.resolve("icon.png")
                        .toAbsolutePath().toString(),

                "--linux-package-name", appName.lowercase(Locale.ROOT).replace(" ", ""),
                "--linux-app-release", "1",

                "--linux-package-deps", "tor",

                "--linux-menu-group", "Network",
                "--linux-shortcut",

                "--linux-deb-maintainer",
                "noreply@bisq.network",
        )

        appContentPaths.forEach { contentPath ->
            arguments.add("--app-content")
            arguments.add(contentPath.toAbsolutePath().toString())
        }

        if (packageFormat == PackageFormat.DEB) {
            arguments.add("--linux-deb-maintainer")
            arguments.add("noreply@bisq.network")

            // Override the deb maintainer scripts (postinst/prerm) to auto-enable Bisq's
            // onion-grater profile on Tails. jpackage still applies its token substitution to
            // these overrides, so the default desktop/service integration is preserved.
            val debResourceDir = resourcesPath.resolve("deb")
            if (Files.isDirectory(debResourceDir)) {
                arguments.add("--resource-dir")
                arguments.add(debResourceDir.toAbsolutePath().toString())
            }
        }

        if (packageFormat == PackageFormat.RPM) {
            arguments.add("--linux-rpm-license-type")
            arguments.add("AGPLv3")
        }

        return arguments
    }
}
