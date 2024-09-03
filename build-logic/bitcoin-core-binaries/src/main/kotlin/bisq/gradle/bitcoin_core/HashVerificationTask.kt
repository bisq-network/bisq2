package bisq.gradle.bitcoin_core

import com.google.common.hash.Hashing
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class HashVerificationTask : DefaultTask() {
    @get:InputFile
    abstract val inputFile: RegularFileProperty

    @get:InputFile
    abstract val sha256File: RegularFileProperty

    @get:OutputFile
    abstract val resultFile: RegularFileProperty

    @TaskAction
    fun verify() {
        val input = inputFile.get().asFile.readBytes()
        val hash: String = Hashing.sha256()
            .hashBytes(input)
            .toString()

        val expectedHash = getExpectedHash()
        if (hash != expectedHash) {
            throw GradleException(
                "Hash verification failed for `${inputFile.get().asFile.absolutePath}`. " +
                        "Expected: `$expectedHash, Actual: `$hash`"
            )
        }

        resultFile.get().asFile.writeText(hash)
    }

    private fun getExpectedHash(): String {
        val inputFileName = inputFile.get().asFile.name
        for (line in sha256File.get().asFile.readLines()) {
            if (line.endsWith(inputFileName)) {
                // '<sha256hash> filename.tar.gz'
                return line.split(" ").first()
            }
        }

        throw GradleException("Couldn't find expected hash for `$inputFile`.")
    }
}