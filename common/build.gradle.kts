plugins {
    java
    id("bisq.java-conventions")
    id("bisq.java-integration-tests")
    id("bisq.protobuf")
}

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

        val gitCommitVersion = try {
            val process = ProcessBuilder("git", "rev-parse", "--short", "HEAD").start()
            process.inputStream.bufferedReader().use { it.readText().trim() }
        } catch (e: Exception) {
            "unknown"
        }
        versionFile.writeText("""
            package bisq.common.application;

            public final class BuildVersion {
                public static final String VERSION = "${project.version}";
                public static final String COMMIT_SHORT_HASH = "$gitCommitVersion";
            }
        """.trimIndent())
    }

    outputs.dir(outputDir)
    inputs.property("version", project.version)
}


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
}
