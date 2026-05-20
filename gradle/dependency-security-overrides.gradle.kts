data class DependencySecurityOverride(
    val version: String,
    val reason: String,
)

val dependencySecurityOverrides = mapOf(
    "org.apache.logging.log4j:log4j-api" to DependencySecurityOverride(
        "2.25.4",
        "Keep Log4j API aligned with the Log4j Core security update.",
    ),
    "org.apache.logging.log4j:log4j-core" to DependencySecurityOverride(
        "2.25.4",
        "Addresses Log4j Core advisories fixed in 2.25.4.",
    ),
    "org.codehaus.plexus:plexus-utils" to DependencySecurityOverride(
        "4.0.3",
        "Addresses CVE-2025-67030 fixed in plexus-utils 4.0.3.",
    ),
)

fun org.gradle.api.artifacts.DependencyResolveDetails.applyDependencySecurityOverride() {
    val override = dependencySecurityOverrides["${requested.group}:${requested.name}"] ?: return
    if (requested.version != override.version) {
        useVersion(override.version)
        because(override.reason)
    }
}

allprojects {
    configurations.configureEach {
        resolutionStrategy.eachDependency {
            applyDependencySecurityOverride()
        }
    }

    buildscript.configurations.configureEach {
        resolutionStrategy.eachDependency {
            applyDependencySecurityOverride()
        }
    }
}
