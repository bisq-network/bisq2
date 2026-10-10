package bisq.gradle.tasks.signature

import org.bouncycastle.bcpg.sig.KeyFlags
import org.bouncycastle.openpgp.PGPException
import org.bouncycastle.openpgp.PGPPublicKey
import org.bouncycastle.openpgp.PGPPublicKeyRing
import org.bouncycastle.openpgp.PGPSignature
import org.bouncycastle.openpgp.PGPUtil
import org.bouncycastle.openpgp.jcajce.JcaPGPPublicKeyRingCollection
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider
import org.bouncycastle.util.encoders.Hex
import org.gradle.api.GradleException
import java.io.ByteArrayInputStream
import java.net.URI
import java.net.URL

class PpgPublicKeyParser(
    private val primaryKeyFingerprint: String,
    private val keyUrl: String
) {

    private var masterKey: PGPPublicKey? = null
    private val subKeys: MutableList<PGPPublicKey> = ArrayList()

    val keyById: Map<Long, PGPPublicKey>
        get() {
            val keyByIdMap = HashMap<Long, PGPPublicKey>()
            keyByIdMap[masterKey!!.keyID] = masterKey!!
            subKeys.forEach { key -> keyByIdMap[key.keyID] = key }
            return keyByIdMap
        }

    fun parse() {
        var url: URL = URI.create(keyUrl).toURL()
        val publicKey: ByteArray = url.readBytes()
        val byteArrayInputStream = ByteArrayInputStream(publicKey)
        PGPUtil.getDecoderStream(byteArrayInputStream)
            .use { decoderInputStream ->
                val publicKeyRingCollection = JcaPGPPublicKeyRingCollection(decoderInputStream)
                parseMasterAndSubKeys(publicKeyRingCollection)

                checkNotNull(masterKey) { "Couldn't find master key." }
                verifyMasterKeyFingerprint()
                verifyMasterKeyNotRevoked()

                if (subKeys.isNotEmpty()) {
                    verifySubKeySignatures()
                }
            }
    }

    private fun parseMasterAndSubKeys(publicKeyRingCollection: JcaPGPPublicKeyRingCollection) {
        val rIt: Iterator<PGPPublicKeyRing> = publicKeyRingCollection.keyRings
        while (rIt.hasNext()) {
            val kRing = rIt.next()
            val kIt = kRing.publicKeys
            while (kIt.hasNext()) {
                val k = kIt.next()

                if (k.isMasterKey) {
                    if (masterKey == null) {
                        masterKey = k
                    } else {
                        throw GradleException("Found multiple find master keys.")
                    }

                } else {
                    subKeys.add(k)
                }
            }
        }
    }

    private fun verifyMasterKeyFingerprint() {
        val fingerprint = Hex.toHexString(masterKey!!.fingerprint)
        if (fingerprint != primaryKeyFingerprint) {
            throw GradleException("$keyUrl has invalid fingerprint.")
        }
    }

    // A revocation of the primary key retires all its subkeys, so no key of the file can be trusted (fail closed)
    private fun verifyMasterKeyNotRevoked() {
        if (masterKey!!.hasRevocation()) {
            throw GradleException("$keyUrl contains a revocation of the primary key. We do not trust any of its keys.")
        }
    }

    // Only a subkey which the master key binds as a signing key, and which is not revoked, can sign a binary. A key file
    // keeps revoked subkeys: the Tor key file holds a signing subkey which was revoked in 2015 as compromised. Such a
    // subkey and a subkey whose binding cannot be verified are not trusted, so a signature of it fails (fail closed).
    private fun verifySubKeySignatures() {
        val untrustedSubKeys = subKeys.filter { subKey -> !isValidSigningSubKey(subKey) }
        untrustedSubKeys.forEach { subKey ->
            println("Subkey `${java.lang.Long.toHexString(subKey.keyID)}` is revoked or not bound as signing key. We do not trust it.")
        }
        subKeys.removeAll(untrustedSubKeys)
    }

    private fun isValidSigningSubKey(subKey: PGPPublicKey): Boolean {
        if (subKey.hasRevocation()) {
            return false
        }
        return subKey.getSignaturesOfType(PGPSignature.SUBKEY_BINDING).asSequence()
            .any { signature -> isSigningSubKeyBinding(signature, subKey) }
    }

    private fun isSigningSubKeyBinding(signature: PGPSignature, subKey: PGPPublicKey): Boolean {
        val keyFlags = signature.hashedSubPackets?.keyFlags ?: 0
        if (keyFlags and KeyFlags.SIGN_DATA == 0) {
            return false
        }
        return try {
            signature.init(JcaPGPContentVerifierBuilderProvider().setProvider("BC"), masterKey!!)
            signature.verifyCertification(masterKey!!, subKey)
        } catch (e: PGPException) {
            false
        }
    }
}