package bisq.gradle.tasks.signature

import org.bouncycastle.bcpg.ArmoredInputStream
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openpgp.PGPPublicKey
import org.bouncycastle.openpgp.PGPSignature
import org.bouncycastle.openpgp.PGPSignatureList
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URL
import java.security.Security

class SignatureVerifier(
    private val pgpFingerprintToKeyUrl: Map<String, URL>
) {

    fun verifySignature(
        fileToVerify: File,
        signatureFile: File,
    ): Boolean {
        Security.addProvider(BouncyCastleProvider())

        val signatureFileInBytes = readSignatureFromFile(signatureFile)
        val pgpObjectFactory = JcaPGPObjectFactory(signatureFileInBytes)
        val signatureList: PGPSignatureList = pgpObjectFactory.nextObject() as PGPSignatureList

        val signatureVerificationResult = mutableListOf<Boolean>()
        pgpFingerprintToKeyUrl.forEach { (fingerprint, keyUrl) ->
            val ppgPublicKeyParser = PpgPublicKeyParser(fingerprint, keyUrl)
            ppgPublicKeyParser.parse()

            val isSignedByAnyKey = verifyDetachedSignature(
                potentialSigningKeys = ppgPublicKeyParser.keyById,
                signatureList = signatureList,
                data = fileToVerify.readBytes()
            )

            signatureVerificationResult.add(isSignedByAnyKey)
        }

        val numberOfSuccessfulVerifications = signatureVerificationResult.filter { isSuccess -> isSuccess }
            .count()
        return numberOfSuccessfulVerifications == signatureList.size()
    }

    private fun readSignatureFromFile(signatureFile: File): ByteArray {
        val signatureByteArray = signatureFile.readBytes()
        val signatureInputStream = ByteArrayInputStream(signatureByteArray)
        val armoredSignatureInputStream = ArmoredInputStream(signatureInputStream)
        return armoredSignatureInputStream.readBytes()
    }

    private fun verifyDetachedSignature(
        potentialSigningKeys: Map<Long, PGPPublicKey>,
        signatureList: PGPSignatureList,
        data: ByteArray
    ): Boolean {
        var pgpSignature: PGPSignature? = null
        var signingKey: PGPPublicKey? = null

        for (s in signatureList) {
            signingKey = potentialSigningKeys[s.keyID]
            if (signingKey != null) {
                pgpSignature = s
                break
            }
        }

        if (signingKey == null) {
            return false
        }

        checkNotNull(pgpSignature) { "signature for key not found" }

        pgpSignature.init(
            JcaPGPContentVerifierBuilderProvider().setProvider("BC"),
            signingKey
        )
        pgpSignature.update(data)
        return pgpSignature.verify()
    }
}