package bisq.gradle.tasks.signature

import org.bouncycastle.openpgp.PGPPublicKey
import org.bouncycastle.openpgp.PGPPublicKeyRing
import org.bouncycastle.openpgp.PGPUtil
import org.bouncycastle.openpgp.jcajce.JcaPGPPublicKeyRingCollection
import java.io.ByteArrayInputStream

object PublicKeyParser {

    fun parsePublicKeyFromFile(publicKey: ByteArray): PGPPublicKey {
        val byteArrayInputStream = ByteArrayInputStream(publicKey)
        PGPUtil.getDecoderStream(byteArrayInputStream)
            .use { decoderInputStream ->
                val publicKeyRingCollection = JcaPGPPublicKeyRingCollection(decoderInputStream)
                return findEncryptionKey(publicKeyRingCollection)
            }
    }

    private fun findEncryptionKey(publicKeyRingCollection: JcaPGPPublicKeyRingCollection): PGPPublicKey {
        var publicKey: PGPPublicKey? = null
        val rIt: Iterator<PGPPublicKeyRing> = publicKeyRingCollection.keyRings
        while (publicKey == null && rIt.hasNext()) {
            val kRing = rIt.next()
            val kIt = kRing.publicKeys
            while (publicKey == null && kIt.hasNext()) {
                val k = kIt.next()
                if (k.isEncryptionKey) {
                    publicKey = k
                }
            }
        }

        checkNotNull(publicKey) { "Cannot find public key." }
        return publicKey
    }
}