import org.gradle.api.GradleException
import org.gradle.api.provider.Provider

fun requiredBuildEnvironmentProperty(propertyName: String, property: Provider<String>): String {
    val value = property.orNull?.trim()
    if (value.isNullOrEmpty()) {
        throw GradleException("Missing $propertyName in gradle.properties")
    }
    return value
}

// A build suffix such as +9 would not be compared, so the minimum may only hold version numbers.
fun minimumJavaVersionNumbers(minimum: String): List<Int> {
    val version = runCatching { Runtime.Version.parse(minimum) }.getOrNull()
    if (version == null || version.pre().isPresent || version.build().isPresent || version.optional().isPresent) {
        throw GradleException("releaseBuild.javaVersion must be a Java version such as 21.0.13.0.3, but is $minimum")
    }
    return version.version()
}

// Compares the version numbers one by one, a missing number counting as 0, so 21.0.13.0.3 is above 21.0.13
fun isAtOrAbove(versionNumbers: List<Int>, minimumNumbers: List<Int>): Boolean {
    for (index in 0 until maxOf(versionNumbers.size, minimumNumbers.size)) {
        val number = versionNumbers.getOrElse(index) { 0 }
        val minimumNumber = minimumNumbers.getOrElse(index) { 0 }
        if (number != minimumNumber) {
            return number > minimumNumber
        }
    }
    return true
}

val expectedBuildJavaVersion = providers.gradleProperty("releaseBuild.javaVersion")
val expectedBuildJavaVendor = providers.gradleProperty("releaseBuild.javaVendor")
val expectedBuildGradleVersion = providers.gradleProperty("releaseBuild.gradleVersion")

val verifyBuildEnvironmentTask = tasks.register("verifyBuildEnvironment") {
    group = "verification"
    description = "Verifies the local Gradle and Java runtime meet the pinned build environment."

    inputs.property("expectedBuildJavaVersion", expectedBuildJavaVersion.orElse(""))
    inputs.property("expectedBuildJavaVendor", expectedBuildJavaVendor.orElse(""))
    inputs.property("expectedBuildGradleVersion", expectedBuildGradleVersion.orElse(""))
    outputs.upToDateWhen { false }

    doLast {
        val expectedJavaVersion = requiredBuildEnvironmentProperty("releaseBuild.javaVersion", expectedBuildJavaVersion)
        val expectedJavaVendor = requiredBuildEnvironmentProperty("releaseBuild.javaVendor", expectedBuildJavaVendor)
        val expectedGradleVersion = requiredBuildEnvironmentProperty("releaseBuild.gradleVersion", expectedBuildGradleVersion)
        val minimumJavaVersionNumbers = minimumJavaVersionNumbers(expectedJavaVersion)
        val actualJavaVersion = Runtime.version()
        val actualJavaVendor = System.getProperty("java.vendor")
        val actualJavaVendorVersion = System.getProperty("java.vendor.version")
        val violations = mutableListOf<String>()

        // releaseBuild.javaVersion is a minimum. Azul builds each update more than once and not for every platform at
        // the same time, so every Azul build of the same feature version at or above the minimum passes. The
        // installers bundle the JDK as their runtime; raising the minimum keeps a JDK with a known flaw out of them.
        val actualJavaVersionNumbers = actualJavaVersion.version()
        if (actualJavaVersion.feature() != minimumJavaVersionNumbers.first() ||
            !isAtOrAbove(actualJavaVersionNumbers, minimumJavaVersionNumbers)) {
            violations += "Java version expected Java ${minimumJavaVersionNumbers.first()} at or above " +
                    "$expectedJavaVersion but was $actualJavaVersion"
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

        // Builds of one release may use different JDK builds, so the log names it
        logger.lifecycle(
            "Verified build environment: Gradle ${gradle.gradleVersion}, Java $actualJavaVersion " +
                    "(${listOfNotNull(actualJavaVendor, actualJavaVendorVersion).joinToString(", ")})."
        )
    }
}

tasks.register("verifyReleaseEnvironment") {
    group = "verification"
    description = "Verifies the local Gradle and Java runtime meet the pinned release build environment."
    dependsOn(verifyBuildEnvironmentTask)
}

tasks.matching { it.name == "check" }.configureEach {
    dependsOn(verifyBuildEnvironmentTask)
}
