import org.gradle.api.GradleException
import org.gradle.api.provider.Provider

fun requiredBuildEnvironmentProperty(propertyName: String, property: Provider<String>): String {
    val value = property.orNull?.trim()
    if (value.isNullOrEmpty()) {
        throw GradleException("Missing $propertyName in gradle.properties")
    }
    return value
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
        val actualJavaVersion = System.getProperty("java.version")
        val actualJavaVendor = System.getProperty("java.vendor")
        val violations = mutableListOf<String>()

        if (actualJavaVersion != expectedJavaVersion) {
            violations += "Java version expected $expectedJavaVersion but was $actualJavaVersion"
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

        logger.lifecycle(
            "Verified build environment: Gradle ${gradle.gradleVersion}, Java $actualJavaVersion ($actualJavaVendor)."
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
