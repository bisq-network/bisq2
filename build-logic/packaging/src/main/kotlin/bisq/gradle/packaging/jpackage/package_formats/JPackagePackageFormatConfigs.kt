package bisq.gradle.packaging.jpackage.package_formats

interface JPackagePackageFormatConfigs {
    val packageFormats: Set<PackageFormat>

    // Arguments which belong to the app image and not to the package, needed by the platforms where the
    // app image is created in its own jpackage call (see PackageFactory).
    val appImageArguments: List<String>
        get() = emptyList()

    fun createArgumentsForJPackage(packageFormat: PackageFormat): List<String>
}
