package bisq.gradle.electrum

import com.google.common.io.BaseEncoding
import java.io.File
import java.security.MessageDigest

object Sha256 {

    private val messageDigest = MessageDigest.getInstance("SHA-256")

    fun hashFile(file: File): String {
        val fileBytes = file.readBytes()
        val hashInBytes = messageDigest.digest(fileBytes)
        return hexEncodeBytes(hashInBytes)
    }

    private fun hexEncodeBytes(bytes: ByteArray): String {
        return BaseEncoding.base16().lowerCase().encode(bytes)
    }
}