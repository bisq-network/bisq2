package bisq.gradle.packaging.jpackage.package_formats

import java.nio.file.Path

class WindowsPackage(private val resourcesPath: Path) : JPackagePackageFormatConfigs {
    override val packageFormats = setOf(PackageFormat.EXE, PackageFormat.MSI)

    override fun createArgumentsForJPackage(packageFormat: PackageFormat): List<String> =
            mutableListOf(
                    // "--resource-dir", resourcesPath.toAbsolutePath().toString(),
                    "--icon", resourcesPath.resolve("Bisq2.ico").toAbsolutePath().toString(),

                    "--win-dir-chooser",
                    "--win-per-user-install",
                    "--win-menu",
                    "--win-shortcut",
            )
}
