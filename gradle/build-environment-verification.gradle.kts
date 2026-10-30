import org.gradle.api.GradleException
import org.gradle.api.provider.Provider

fun requiredBuildEnvironmentProperty(propertyName: String, property: Provider<String>): String {
    val value = property.orNull?.trim()
    if (value.isNullOrEmpty()) {
        throw GradleException("Missing $propertyName in gradle.properties")
    }
    return value
}

// A build suffix such as +9 would not be compared, so the pin may only hold the numbers of the update.
fun pinnedJavaVersionNumbers(pin: String): List<Int> {
    val version = runCatching { Runtime.Version.parse(pin) }.getOrNull()
    if (version == null || version.pre().isPresent || version.build().isPresent || version.optional().isPresent) {
        throw GradleException("releaseBuild.javaVersion must be a Java update such as 21.0.13, but is $pin")
    }
    return version.version()
}

val expectedBuildJavaVersion = providers.gradleProperty("releaseBuild.javaVersion")
val expectedBuildJavaVendor = providers.gradleProperty("releaseBuild.javaVendor")
val expectedBuildGradleVersion = providers.gradleProperty("releaseBuild.gradleVersion")

val verifyBuildEnvironmentTask = tasks.register("verifyBuildEnvironment") {
    group = "verification"
    description = "Verifies the local Gradle and Java runtime match the pinned build environment."

    inputs.property("expectedBuildJavaVersion", expectedBuildJavaVersion.orElse(""))
    inputs.property("expectedBuildJavaVendor", expectedBuildJavaVendor.orElse(""))
    inputs.property("expectedBuildGradleVersion", expectedBuildGradleVersion.orElse(""))
    outputs.upToDateWhen { false }

    doLast {
        val expectedJavaVersion = requiredBuildEnvironmentProperty("releaseBuild.javaVersion", expectedBuildJavaVersion)
        val expectedJavaVendor = requiredBuildEnvironmentProperty("releaseBuild.javaVendor", expectedBuildJavaVendor)
        val expectedGradleVersion = requiredBuildEnvironmentProperty("releaseBuild.gradleVersion", expectedBuildGradleVersion)
        val expectedJavaVersionNumbers = pinnedJavaVersionNumbers(expectedJavaVersion)
        val actualJavaVersion = Runtime.version()
        val actualJavaVendor = System.getProperty("java.vendor")
        val actualJavaVendorVersion = System.getProperty("java.vendor.version")
        val violations = mutableListOf<String>()

        // Azul builds a Java update more than once, for example 21.0.13 as 21.0.13+9 and as 21.0.13.0.3+1, and an
        // installer may offer only the latest. The builds compile the same classes, so every build of the update
        // passes. The installers bundle the JDK as their runtime, so verifyReleaseEnvironment of the root build also
        // checks the exact build.
        if (actualJavaVersion.version().take(expectedJavaVersionNumbers.size) != expectedJavaVersionNumbers) {
            violations += "Java version expected a build of $expectedJavaVersion but was $actualJavaVersion"
        }
        if (actualJavaVendor != expectedJavaVendor) {
            violations += "Java vendor expected $expectedJavaVendor but was $actualJavaVendor"
        }
        if (gradle.gradleVersion != expectedGradleVersion) {
            violations += "Gradle version expected $expectedGradleVersion but was ${gradle.gradleVersion}"
        }

        if (violations.isNotEmpty()) {
            throw GradleException(
                "Build environment verification failed:\n" +
                        violations.joinToString("\n") { "  - $it" }
            )
        }

        // The build of the JDK is not checked, so the log names it
        logger.lifecycle(
            "Verified build environment: Gradle ${gradle.gradleVersion}, Java $actualJavaVersion " +
                    "(${listOfNotNull(actualJavaVendor, actualJavaVendorVersion).joinToString(", ")})."
        )
    }
}

tasks.register("verifyReleaseEnvironment") {
    group = "verification"
    description = "Verifies the local Gradle and Java runtime match the pinned release build environment."
    dependsOn(verifyBuildEnvironmentTask)
}

tasks.matching { it.name == "check" }.configureEach {
    dependsOn(verifyBuildEnvironmentTask)
}
