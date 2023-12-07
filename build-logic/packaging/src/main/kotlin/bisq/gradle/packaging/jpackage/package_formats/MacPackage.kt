package bisq.gradle.packaging.jpackage.package_formats

import java.nio.file.Path

class MacPackage(private val resourcesPath: Path) : JPackagePackageFormatConfigs {
    override val packageFormats = setOf(PackageFormat.DMG)

    override fun createArgumentsForJPackage(packageFormat: PackageFormat): List<String> =
            mutableListOf(
                    "--resource-dir", resourcesPath.toAbsolutePath().toString(),
                    "--mac-package-name", "Bisq 2",
                    "--icon", resourcesPath.resolve("Bisq2.icns").toAbsolutePath().toString()
            )
}
