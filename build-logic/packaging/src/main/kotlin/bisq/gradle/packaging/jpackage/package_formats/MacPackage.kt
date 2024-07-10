package bisq.gradle.packaging.jpackage.package_formats

import java.nio.file.Path

class MacPackage(private val resourcesPath: Path, private val appName: String) : JPackagePackageFormatConfigs {
    override val packageFormats = setOf(PackageFormat.DMG)

    override fun createArgumentsForJPackage(packageFormat: PackageFormat): List<String> {
        // App name is "Bisq" or "Bisq2"
        val iconFileName = "${appName.replace(" ", "")}.icns"
        return mutableListOf(
            "--resource-dir", resourcesPath.toAbsolutePath().toString(),
            "--mac-package-name", appName,
            "--icon", resourcesPath.resolve(iconFileName).toAbsolutePath().toString(),
        )
    }
}
