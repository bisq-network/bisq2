import bisq.gradle.common.getPlatform
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.util.Properties
import javax.inject.Inject

plugins {
    java
    id("bisq.java-conventions")
    id("bisq.java-integration-tests")
    id("bisq.protobuf")
}

// The Tor build in the Git submodule tor/ defines the Tor versions, so that they exist only once
val torGradleProperties = Properties().apply {
    rootProject.file("tor/gradle.properties").inputStream().use { load(it) }
}

// The Tor Browser release whose expert bundle the Tor build packages for this platform: tor.version, unless the platform
// takes the bundle of another release, as Linux ARM64 takes an alpha release
val torVersion: String? = torGradleProperties.getProperty("tor.bundleVersion.${getPlatform().platformName}")
    ?: torGradleProperties.getProperty("tor.version")
if (torVersion == null) {
    throw GradleException("Tor version is not defined in tor/gradle.properties")
}

val torBinaryVersion: String? = torGradleProperties.getProperty("tor.binaryVersion")
if (torBinaryVersion == null) {
    throw GradleException("Tor binary version is not defined in tor/gradle.properties")
}

/**
 * The current commit, cut to a fixed length, as the length of git's --short grows with the number of objects in the
 * clone. A value source rather than a plain provider, so it is also read again on each build with the configuration
 * cache.
 */
abstract class GitCommitShortHash : ValueSource<String, GitCommitShortHash.Parameters> {
    interface Parameters : ValueSourceParameters {
        val projectDir: DirectoryProperty
    }

    // Reads stdout and stderr at the same time, so git cannot block on a full pipe
    @get:Inject
    abstract val execOperations: ExecOperations

    override fun obtain(): String {
        val logger = Logging.getLogger(GitCommitShortHash::class.java)
        val output = ByteArrayOutputStream()
        val error = ByteArrayOutputStream()
        try {
            val result = execOperations.exec {
                commandLine("git", "rev-parse", "HEAD")
                workingDir(parameters.projectDir.get().asFile)
                standardOutput = output
                errorOutput = error
                isIgnoreExitValue = true
            }
            if (result.exitValue == 0) {
                return output.toString().trim().take(10)
            }
            logger.warn("Using 'unknown' as the commit hash, git rev-parse HEAD failed: {}", error.toString().trim())
        } catch (e: Exception) {
            // Expected where git is not installed, like in the api-app Docker build, so the stack trace is info only
            logger.warn("Using 'unknown' as the commit hash, git could not be run: {}", e.message)
            logger.info("git could not be run", e)
        }
        return "unknown"
    }
}

// Builds without a git checkout, like the api-app Docker image, pass the commit as -PbuildCommit=<commit hash>. It is
// written into a properties file, so only a plain hash is accepted.
val buildCommitProperty = providers.gradleProperty("buildCommit")
    .filter { it.isNotBlank() }
    .map { commit ->
        if (!commit.matches(Regex("[0-9a-f]{10,40}"))) {
            throw GradleException("buildCommit must be a commit hash of at least 10 characters, but was '$commit'")
        }
        commit.take(10)
    }

val commitShortHash = buildCommitProperty.orElse(providers.of(GitCommitShortHash::class) {
    parameters.projectDir.set(layout.projectDirectory)
})

/**
 * Generate a Java class with the current version number extracted from gradle.properties and makes
 * it available for use in all java modules that has access to common.
 */
val generateVersionClass by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/source/version").get().asFile
    val versionFile = file("${outputDir}/bisq/common/application/BuildVersion.java")

    doLast {
        outputDir.mkdirs()
        versionFile.parentFile.mkdirs()

        versionFile.writeText("""
            package bisq.common.application;

            public final class BuildVersion {
                public static final String VERSION = "${project.version}";
                public static final String TOR_VERSION = "$torVersion";
                public static final String TOR_BINARY_VERSION = "$torBinaryVersion";
            }
        """.trimIndent())
    }

    outputs.dir(outputDir)
    inputs.property("version", project.version)
    inputs.property("torVersion", torVersion)
    inputs.property("torBinaryVersion", torBinaryVersion)
}

/**
 * Writes the commit hash to a resource that ApplicationVersion reads at runtime. As a constant in BuildVersion it
 * would change the classes of common with every commit, and every test task using common would rerun. The
 * bisq.java-conventions plugin excludes this resource from runtime classpath checks for the same reason.
 */
val generateBuildCommitResource by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/resources/build-commit").get().asFile
    val resourceFile = file("${outputDir}/bisq/common/application/build-commit.properties")

    doLast {
        resourceFile.parentFile.mkdirs()
        resourceFile.writeText("commitShortHash=${commitShortHash.get()}\n")
    }

    outputs.dir(outputDir)
    inputs.property("commitShortHash", commitShortHash)
}

sourceSets["main"].resources.srcDir(generateBuildCommitResource)


/**
 * Add generated source directories to the source set so they are compiled alongside the main source set.
 */
tasks.named<JavaCompile>("compileJava") {
    dependsOn(generateVersionClass)
    source(layout.buildDirectory.dir("generated/source/version"))
}

sourceSets["main"].java.srcDir(layout.buildDirectory.dir("generated/source/version"))

dependencies {
    implementation(libs.typesafe.config)
    implementation(libs.annotations)
    implementation(libs.google.libphonenumber)
    implementation(libs.bundles.jackson)
    implementation(libs.swagger.jaxrs2.jakarta)
    implementation(libs.bundles.glassfish.jersey)
}
