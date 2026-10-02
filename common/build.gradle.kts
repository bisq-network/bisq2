import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import javax.inject.Inject

plugins {
    java
    id("bisq.java-conventions")
    id("bisq.java-integration-tests")
    id("bisq.protobuf")
}


val torVersion: String? = project.findProperty("tor.version") as String?
if (torVersion == null) {
    throw GradleException("Tor version is not defined in gradle.properties")
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
            logger.warn("Using 'unknown' as the commit hash, git could not be run", e)
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
            }
        """.trimIndent())
    }

    outputs.dir(outputDir)
    inputs.property("version", project.version)
    inputs.property("torVersion", torVersion)
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
