package bisq.gradle.electrum.tasks

import bisq.gradle.electrum.SignatureVerifier
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import java.net.URL

abstract class FileVerificationTask : DefaultTask() {

    @get:InputFile
    abstract val fileToVerify: RegularFileProperty

    @get:InputFile
    abstract val detachedSignatureFile: RegularFileProperty

    @get:Input
    abstract val publicKeyUrls: SetProperty<URL>

    @get:Input
    abstract val publicKeyFingerprints: SetProperty<String>

    @TaskAction
    fun verify() {
        val signatureVerifier = SignatureVerifier(
            allPublicKeyUrls = publicKeyUrls.get(),
            publicKeyFingerprints = publicKeyFingerprints.get()
        )

        val isSignatureValid = signatureVerifier.verifySignature(
            signatureFile = detachedSignatureFile.get().asFile,
            fileToVerify = fileToVerify.get().asFile
        )

        if (!isSignatureValid) {
            throw GradleException(
                "Signature verification failed for ${fileToVerify.get().asFile.absolutePath}."
            )
        }
    }
}