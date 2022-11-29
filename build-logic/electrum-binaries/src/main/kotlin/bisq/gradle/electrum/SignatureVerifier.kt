package bisq.gradle.electrum

import org.bouncycastle.bcpg.ArmoredInputStream
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openpgp.PGPPublicKey
import org.bouncycastle.openpgp.PGPSignature
import org.bouncycastle.openpgp.PGPSignatureList
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider
import org.bouncycastle.util.encoders.Hex
import org.gradle.api.GradleException
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URL
import java.security.Security

class SignatureVerifier(
    private val allPublicKeyUrls: Set<URL>,
    private val publicKeyFingerprints: Set<String>
) {

    fun verifySignature(
        fileToVerify: File,
        signatureFile: File,
    ): Boolean {
        Security.addProvider(BouncyCastleProvider())

        var isSuccess = true
        for (publicKeyUrl in allPublicKeyUrls) {
            val publicKey: PGPPublicKey = parseAndVerifyPublicKey(publicKeyUrl)
            val signature: ByteArray = readSignatureFromFile(signatureFile)
            val fileToVerifyByteArray: ByteArray = fileToVerify.readBytes()

            val hasValidSignature = verifyDetachedSignature(publicKey, signature, fileToVerifyByteArray)
            isSuccess = isSuccess && hasValidSignature
        }

        return isSuccess
    }

    private fun parseAndVerifyPublicKey(
        publicKeyUrl: URL
    ): PGPPublicKey {
        val publicKeyByteArray = publicKeyUrl.readBytes()
        val publicKey: PGPPublicKey = PublicKeyParser.parsePublicKeyFromFile(publicKeyByteArray)

        val fingerprint = Hex.toHexString(publicKey.fingerprint)
        if (fingerprint !in publicKeyFingerprints) {
            throw GradleException("$publicKeyUrl has invalid fingerprint.")
        }

        return publicKey
    }

    private fun readSignatureFromFile(signatureFile: File): ByteArray {
        val signatureByteArray = signatureFile.readBytes()
        val signatureInputStream = ByteArrayInputStream(signatureByteArray)
        val armoredSignatureInputStream = ArmoredInputStream(signatureInputStream)
        return armoredSignatureInputStream.readBytes()
    }

    private fun verifyDetachedSignature(
        verifyingKey: PGPPublicKey,
        pgpSignatureByteArray: ByteArray,
        data: ByteArray
    ): Boolean {
        val pgpObjectFactory = JcaPGPObjectFactory(pgpSignatureByteArray)
        val signatureList: PGPSignatureList = pgpObjectFactory.nextObject() as PGPSignatureList

        var pgpSignature: PGPSignature? = null
        for (s in signatureList) {
            if (s.keyID == verifyingKey.keyID) {
                pgpSignature = s
                break
            }
        }
        checkNotNull(pgpSignature) { "signature for key not found" }
        pgpSignature.init(
            JcaPGPContentVerifierBuilderProvider().setProvider("BC"),
            verifyingKey
        )
        pgpSignature.update(data)
        return pgpSignature.verify()
    }
}