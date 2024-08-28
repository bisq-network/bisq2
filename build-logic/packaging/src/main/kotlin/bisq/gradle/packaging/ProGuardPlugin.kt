package bisq.gradle.packaging

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*
import java.io.File
import java.nio.file.Paths
import java.util.Properties
import java.util.jar.JarFile
import java.util.zip.ZipFile

/**
 * Allows to configure relativity to proguard config file (root) from any composite submodule build.gradle.kts
 */
open class ProGuardExtension {
    var rulesFileRelativePath: String? = null
    var enabled: Boolean = true
}
/**
 * A Gradle plugin that applies ProGuard to obfuscate Java bytecode in leaf projects.
 *
 * This plugin is designed to be applied to a Gradle root and will configure ProGuard tasks for each leaf project.
 * The plugin will only apply ProGuard to leaf projects that have no subprojects and are not blacklisted.
 * On gradle composite setups like the one we use, you need to apply it to each composite "root"
 *
 * The plugin will create a "proguardTask" for each leaf project and configure it to run ProGuard on the project's JAR files.
 * The plugin will also configure the "jar" task to depend on the "proguardTask", ensuring that the obfuscated JAR is generated.
 *
 * The plugin will log information about the leaf projects it applies ProGuard to and will provide warnings for any leaf projects
 * that are blacklisted or have subprojects.
 *
 * The plugin will also provide a "listJars" task that can be used to list the JAR files in the "build/libs" directory of each leaf project.
 *
 * @author Rodrigo Varela
 */
class ProGuardPlugin : Plugin<Project> {
    companion object {
        val BLACKLISTED = listOf("platform", "i2p")
        const val UNPROGUARDED_KEY = "unproguarded"
    }

    override fun apply(project: Project) {
        val extension = project.extensions.create("proguardConfig", ProGuardExtension::class.java)

        if (extension.rulesFileRelativePath == null) {
            project.logger.warn("WARNING: rules file path not configured, using ${project.rootDir.path}")
            extension.rulesFileRelativePath = project.rootDir.path
            // Fix for build servers (github)
            if (project.rootDir.path.endsWith("/apps")) {
                extension.rulesFileRelativePath = project.rootDir.path.replace("/apps", "")
            }
        }
        val propertiesFile = File(extension.rulesFileRelativePath, "gradle.properties")
        if (propertiesFile.exists()) {
            val properties = Properties().apply { load(propertiesFile.inputStream()) }
            extension.enabled = properties.getProperty("proguard.enable", "true").toBoolean()
        } else {
            project.logger.warn("gradle.properties not found at: ${extension.rulesFileRelativePath}")
        }

        val configured = mutableSetOf<String>()
        setupListJars(project)
        project.afterEvaluate {
            project.allprojects {
                configureProGuardForLeafProjects(project, extension, configured)
            }
        }
    }

    private fun configureProGuardForLeafProjects(project: Project,
                                                 extension: ProGuardExtension,
                                                 configured: MutableSet<String>) {
        if (project.subprojects.isEmpty()
            && !BLACKLISTED.contains(project.name)) {

            if (!configured.contains(project.name)) {
                setupListJars(project)
                configured.add(project.name)
            }
//            println("${project.name} is a leaf project.")


            var proguardTask = project.tasks.findByName("proguardTask")
            if (proguardTask == null) {
                project.tasks.register<JavaExec>("proguardTask") {

                }
                proguardTask = project.tasks.findByName("proguardTask")
            }
            (proguardTask as JavaExec).apply {
                //            dependsOn("build") // Ensure JARs are built before running ProGuard
                mainClass.set("proguard.ProGuard")
                group = "build"
                description = "Runs ProGuard to obfuscate the code."
                enabled = extension.enabled

                val classpathConfig = project.configurations.findByName("runtimeClasspath")
                    ?: project.configurations.findByName("compileClasspath")
                    ?: project.configurations.findByName("default")

                if (classpathConfig != null) {
                    classpath = classpathConfig
//                    println("Using classpath ${classpathConfig.name} for project ${project.name}")
                } else {
//                    println("WARNING: No suitable classpath found for project ${project.name}")
                }

                doFirst {
                    var inputDir = File("${project.layout.buildDirectory.get()}/libs")
                    var outputDir = File("${project.layout.buildDirectory.get()}/libs/${UNPROGUARDED_KEY}")
                    val originalFiles =
                        inputDir.listFiles { file -> file.extension == "jar" }
                            ?.filter { hasClasses(it) } ?: emptyList()
                    // original jar task output dir

                    if (!outputDir.exists()) {
                        outputDir.mkdirs()
                    }
                    originalFiles.forEach { jarFile ->
                        val targetFile = File(outputDir, "${jarFile.nameWithoutExtension}.jar")
                        try {
                            jarFile.copyTo(targetFile, overwrite = true)
                            println("Copied ${jarFile.absolutePath} to ${targetFile.absolutePath}")
                        } catch (e: Exception) {
                            println("WARNING: Failed to copy ${jarFile.absolutePath} to ${targetFile.absolutePath} : ${e.message}")
                        }
                    }

                    // Configure ProGuard task for this leaf project
                    inputDir = File("${project.layout.buildDirectory.get()}/libs/${UNPROGUARDED_KEY}")
                    outputDir = File("${project.layout.buildDirectory.get()}/libs")
                    val rulesFile = File("${extension.rulesFileRelativePath}/proguard-rules.pro".replace("//", "/"))

                    // Collect library JARs from runtimeClasspath - needed for tests to pass
                    val myClasspath = project.configurations.findByName("runtimeClasspath")
                        ?: project.configurations.findByName("compileClasspath")
                        ?: project.configurations.findByName("default")
                    val libraryJars = myClasspath?.filter { it.name.endsWith(".jar") && hasClasses(it) }
                        ?.map { it.absolutePath } ?: emptyList()


                    // Detect the Java runtime library path
                    // this is very important to avoid proguard "Cannot find symbol" nightmare warnings
                    val javaHome = System.getProperty("java.home")
                    val javaRuntimeJars = listOf(
                        Paths.get(javaHome, "jmods", "java.base.jmod").toFile(),
                        Paths.get(javaHome, "jmods", "jdk.management.jmod").toFile(),  // For java.lang.management
                        Paths.get(javaHome, "jmods", "jdk.unsupported.jmod").toFile(),  // For sun.nio and other internal classes
                        Paths.get(javaHome, "jmods", "jdk.httpserver.jmod").toFile(),  // For sun.nio and other internal classes
                        Paths.get(javaHome, "jmods", "java.desktop.jmod").toFile(),   // For javax and swing and other internal classes
                    )

                    val libClasspathLogWhitelist = listOf("protobuf", "common")

                    libraryJars.forEach { libName ->
                        if (libClasspathLogWhitelist.any { libName.contains(it) })
                            println("library used: $libName")
                    }

                    // Prepare arguments for ProGuard
                    // use the original targe dir to get jar names
                    val inputJars =
                        outputDir.listFiles { file -> file.extension == "jar" }?.filter { hasClasses(it) } ?: emptyList()

                    // Prepare arguments for ProGuard
                    args = inputJars.flatMap { jarFile ->
                        listOf(
                            "-injars", "${inputDir}/${jarFile.nameWithoutExtension}.jar",
                            "-outjars", "${outputDir}/${jarFile.nameWithoutExtension}.jar",
                        )
                    } + libraryJars.flatMap { jarPath ->
                        listOf(
                            "-libraryjars", jarPath
                        )
                    } + javaRuntimeJars.flatMap { jarPath ->
                        listOf(
                            "-libraryjars", jarPath.absolutePath
                        )
                    } + listOf(
                        "@${rulesFile}" // Path to ProGuard rules file
                    )
                }
                onlyIf {
                    // use output dir because files haven't been moved at this point
                    (File("${project.layout.buildDirectory.get()}/libs").listFiles { file -> file.extension == "jar" }?.filter { hasClasses(it) } ?: emptyList()).isNotEmpty()
                }
            }

            if (extension.enabled) {
                project.apply(plugin = "java")

                project.repositories {
                    mavenCentral()
                }
                // Ensure the ProGuard task is finalized by the build task
                val jarTask = project.tasks.findByName("jar")
                if (jarTask != null) {
                    // Ensure the ProGuard task is finalized by the build task
                    project.tasks.named<Jar>("jar") {
                        finalizedBy("proguardTask")
                        //project.subprojects.forEach {
                        //    mustRunAfter(it.tasks.named("proguardTask"))
                        //}
                    }

                    setupTestDependencies(project)
                } else {
                    println("WARNING: No 'build' task found in ${project.name}. Cannot bind proguard task to build for this case")
                }
            }
        } else {
            // Recursive case: Iterate over subprojects
            for (subproject in project.subprojects) {
                configureProGuardForLeafProjects(subproject, extension, configured)
            }
        }
    }

    /**
     * Sets up test dependencies for the given project.
     *
     * This function is specifically designed to handle the "persistence" project and its dependency on the "common" project.
     * If the given project is named "persistence", it configures the "test" task to run after the "proguardTask" of the "common" project.
     * Additionally, it optionally ensures that the "test" task waits for the entire build process of the "common" module.
     *
     * IMPORTANT NOTE: persistence:test won't pass in default multi-thread gradle setup without this tweak
     *
     * @param project The Gradle project for which to set up test dependencies
     */
    private fun setupTestDependencies(project: Project) {
        try {
            if (!BLACKLISTED.contains(project.name)) {
                project.tasks.named("test") {
                    val commonProject = project.rootProject.findProject(":common")
                    if (commonProject != null && project.name != "common") {
                        // Ensure persistence:test starts after common:proguardTask
                        mustRunAfter(commonProject.tasks.named("proguardTask"))

                        // Optionally ensure it also waits for the entire build process of the common module
                        dependsOn(commonProject.tasks.named("build"))
                    }
                    //mustRunAfter(project.tasks.named("proguardTask"))
                }

            }
        } catch (e: Exception) {
            println("WARNING: Project ${project.name} doesn't have common dependency")
        }
    }

    private fun setupListJars(project: Project) {
        val listJarsTask = project.tasks.findByName("listJars")
        if (listJarsTask == null) {
            project.tasks.register("listJars") {
                doLast {
                    printJarWithSize(project)
                }
            }
        } else {
            project.tasks.named("listJars").configure {
                doLast {
                    printJarWithSize(project)
                }
            }
        }
    }

    private fun hasClasses(jarFile: File): Boolean {
        try {
            verifyReadableZip(jarFile)
            JarFile(jarFile).use { jar ->
                return jar.entries().asSequence().any { it.name.endsWith(".class") }
            }
        } catch (e: Exception) {
            println("ERROR: Error checking for classes in ${jarFile.absolutePath}: ${e.message}")
            return false
        }
    }

    private fun verifyReadableZip(jarFile: File) {
        if (!jarFile.exists()) {
            println("File does not exist.")
            throw IllegalArgumentException("File does not exist")
        }

        try {
            ZipFile(jarFile).use { zip ->
                zip.entries().asSequence().filter { !it.isDirectory }.forEach { entry ->
                    zip.getInputStream(entry).use { inputStream ->
                        inputStream.readBytes() // Read the bytes to ensure the entry is readable
                    }
                }
            }
            //println("ZIP file is intact.")
        } catch (e: Exception) {
            println("ZIP file is corrupted or cannot be opened.")
            throw e
        }
    }

    private fun printJarWithSize(project: Project) {
        var size = 0.0
        var unproguardSize = 0.0
        project.file("build/libs").listFiles()?.forEach { file ->
            try {
                var fileToPrint = file
                var unobfuscated = false
                if (fileToPrint.absolutePath.contains(UNPROGUARDED_KEY)) {
                    unobfuscated = true
                    fileToPrint = File(file.absolutePath).listFiles()?.ifEmpty { arrayOf(file) }?.first() ?: file
                }
                val fileSizeInMB = fileToPrint.length() / (1024.0)
                if (unobfuscated) {
                    unproguardSize = fileSizeInMB;
                } else {
                    size = fileSizeInMB;
                }
                println("${project.name}: ${if (unobfuscated) "${UNPROGUARDED_KEY}/" else ""}${fileToPrint.name} (${String.format("%.2f", fileSizeInMB)} KB)")
            } catch (e: Exception) {
                println("ERROR: Error printing ${e.message}")
            }
        }
        if (unproguardSize > 0.0) {
            println("${project.name}: Size optimization: ${String.format("%.2f", 100 * (unproguardSize - size) / unproguardSize )}%")
        }
    }
}