package bisq.gradle.packaging.jpackage

data class JPackageAppConfig(
        val name: String,
        val appVersion: String,
        val mainJarFileName: String,
        val mainClassName: String,
        val jvmArgs: Set<String>,
        val licenceFilePath: String
)
