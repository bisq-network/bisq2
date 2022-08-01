package bisq.gradle.electrum

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import java.io.File

abstract class ElectrumBinaryHashes {
    @get:Input
    abstract val appImageHash: Property<String>

    @get:Input
    abstract val dmgHash: Property<String>

    @get:Input
    abstract val exeHash: Property<String>

    fun getHashPropertyForBinary(binary: File): Property<String> {
        // binaryName: electrum-4.2.2.dmg
        val binaryName = binary.name
        return when (val binarySuffix = binaryName.split(".").last()) {
            "AppImage" -> {
                appImageHash
            }
            "dmg" -> {
                dmgHash
            }
            "exe" -> {
                exeHash
            }
            else -> {
                throw IllegalStateException("Unknown binary suffix: $binarySuffix")
            }
        }
    }
}