package bisq.gradle.packaging

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import java.io.BufferedReader
import java.io.InputStreamReader


//import java.util.regex.Pattern

abstract class JLinkTask : DefaultTask() {

//    companion object {
//        val pattern: Pattern = Pattern.compile(".*\\b(java\\..*|javax\\..*)\\b.*\$")
//    }

    @get:InputDirectory
    abstract val jdkDirectory: DirectoryProperty

    @get:Optional
    @get:InputDirectory
    abstract val javaModulesDirectory: DirectoryProperty

    @get:InputFile
    abstract val jDepsOutputFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun run() {
        // Ensure the output directory is clean
        val outputDirectoryFile = outputDirectory.asFile.get()
        if (outputDirectoryFile.exists() && outputDirectoryFile.listFiles()?.isNotEmpty() == true) {
            // In case you want to run it manually, this will stop the plugin from running the task
            logger.lifecycle("custom jre already created, skipping - see: ${jdkDirectory.get()}")
        } else {
            if (!outputDirectoryFile.canWrite()) {
                throw IllegalStateException("Output directory is not writable: ${outputDirectoryFile.absolutePath}")
            }
            outputDirectoryFile.deleteRecursively()

            logger.lifecycle("JDK path: ${jdkDirectory.get()}")

            // Construct the jlink command
            val jLinkPath = jdkDirectory.asFile.get().toPath().resolve("bin").resolve("jlink")
            val processBuilder = ProcessBuilder(
                jLinkPath.toAbsolutePath().toString(),
                "--verbose",
                "--add-modules", parseUsedJavaModulesFromJDepsOutput(),
                "--include-locales=en,cs,de,es,it,pcm,pt-BR",
                "--strip-native-commands",
                "--no-header-files",
                "--no-man-pages",
                "--compress=2",
//            "--strip-debug", // makes jlink fail in some platforms like Ubuntu linux
//            "--add-reads", "java.base=ALL-UNNAMED",
                "--output", outputDirectoryFile.absolutePath
            )
            logger.lifecycle("Executing jlink command: ${processBuilder.command().joinToString(" ")}")

            // Add module path if specified
            if (javaModulesDirectory.isPresent) {
                val commands = processBuilder.command()
                commands.add("--module-path")
                commands.add(javaModulesDirectory.asFile.get().absolutePath)
                logger.lifecycle("Executing jlink command: ${commands.joinToString(" ")}")
            }

            logger.lifecycle("Preparing process builder..")
            processBuilder.inheritIO()

            logger.lifecycle("Starting process builder..")
            val process = processBuilder.start()
            if (System.getProperty("os.name").lowercase().contains("windows")) {
                val reader =
                    BufferedReader(InputStreamReader(process.inputStream))
                while ((reader.readLine()) != null) {
                    // WORKAROUND: Jlink hangs in windows (11pro) when running from this custom plugin
                    // This buffer reader is needed jus to get it going and won't affect the rest of the platforms
                }
            }
            logger.lifecycle("Reading errors..")
            val errorOutput = process.errorStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            if (exitCode != 0) {
                logger.error("jlink error output:\n$errorOutput")
                throw IllegalStateException("jlink couldn't create custom runtime. Exit code: $exitCode")
            }
        }
    }

    private fun parseUsedJavaModulesFromJDepsOutput(): String {
//        TODO instead of hardcoding he modules process the jdeps report and generate the below so we don't need
//             to touch this code again in case of needed new jmods in the future
//            readLines.filter { pattern.matcher(it).matches() }
//            .flatMap { line -> line.split("->").map { it.trim() } }
//            .filter { it.startsWith("java.") || it == "bisq.desktop_app_launcher" }
//            .flatMap { line -> line.split(",").map { it.trim() } }
//            .distinct()
//            .joinToString(",")
//        val readLines = jDepsOutputFile.asFile.get().readLines()
        val modules = listOf(
            // base
            "java.base",
            "java.datatransfer",
            "java.desktop",
            "java.logging",
            "java.xml",
            "java.naming",
            "java.net.http",
            // security
            "java.se",
            "java.security.jgss",
            // javafx
            "javafx.base",
            "javafx.controls",
            "javafx.fxml",
            "javafx.graphics",
            "javafx.media",
            "javafx.swing",
//            "javafx.web",
            // jdk
            "jdk.unsupported",
            "jdk.localedata",
            "jdk.crypto.cryptoki",
            "jdk.crypto.ec"
        ).joinToString(",")
        logger.lifecycle("Modules to be included: $modules")
        return modules
    }
}