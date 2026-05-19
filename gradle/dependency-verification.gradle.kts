import org.gradle.api.GradleException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.kotlin.dsl.register
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.File
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneOffset
import java.util.Base64
import java.util.Locale
import java.util.TreeMap
import java.util.TreeSet
import javax.xml.parsers.DocumentBuilderFactory

data class OpenPgpPacket(val tag: Int, val body: ByteArray)
data class ResolvableConfiguration(val projectPath: String, val configuration: Configuration)
data class ResolvedBuildDependencies(
    val resolvedComponents: Set<String>,
    val directComponents: Set<String>,
    val configurationCount: Int,
    val artifactsByComponent: Map<String, Set<String>> = emptyMap(),
)
data class SignerMetadata(
    val keyId: String,
    val name: String,
    val email: String,
    val userId: String,
    val created: String,
)
data class ChecksumFallbackAllowlist(
    val entries: Set<String>,
    val rationales: Map<String, String>,
)
data class TrustedKeyEntry(
    val group: String,
    val name: String,
    val version: String,
    val regex: Boolean,
)
data class TrustedKey(val id: String, val entries: List<TrustedKeyEntry>)
data class VerifiedArtifact(
    val name: String,
    val checksumOnly: Boolean,
    val checksumFallbackReason: String,
    val keyIds: List<String>,
)
data class VerifiedComponent(
    val id: String,
    val group: String,
    val name: String,
    val version: String,
    val artifacts: List<VerifiedArtifact>,
)
data class DependencyReportRow(
    val id: String,
    val scope: String,
    val status: String,
    val signedCount: Int,
    val checksumOnlyCount: Int,
    val keyIds: List<String>,
    val checksumArtifacts: List<Pair<String, String>>,
)

fun findBisqRepositoryRoot(start: File): File {
    var current: File? = start.canonicalFile
    while (current != null) {
        if (File(current, "gradle/libs.versions.toml").isFile && File(current, "settings.gradle.kts").isFile) {
            return current
        }
        current = current.parentFile
    }
    throw GradleException("Could not locate Bisq repository root from $start")
}

val bisqRepositoryRoot = findBisqRepositoryRoot(rootDir)
val isBisqRepositoryRootBuild = rootDir.canonicalFile == bisqRepositoryRoot.canonicalFile
val dependencyVerificationMetadata = File(bisqRepositoryRoot, "gradle/verification-metadata.xml")
val dependencyVerificationKeyring = File(bisqRepositoryRoot, "gradle/verification-keyring.keys")
val dependencyVerificationBinaryKeyring = File(bisqRepositoryRoot, "gradle/verification-keyring.gpg")
val dependencyChecksumFallbackAllowlist = File(bisqRepositoryRoot, "gradle/dependency-checksum-fallback-allowlist.tsv")
val dependencySignatureReport = File(bisqRepositoryRoot, "docs/dependency-signature-report.md")
val dependencyVerificationInventoryDir = File(bisqRepositoryRoot, "build/dependency-verification")
val gradleWrapperCommand =
    if (System.getProperty("os.name").lowercase(Locale.ROOT).contains("windows")) "gradlew.bat" else "./gradlew"
val includedBuildCurrentResolverTaskPaths = listOf(
    ":build-logic:resolveAndVerifyDependenciesForCurrentBuild",
    ":apps:resolveAndVerifyDependenciesForCurrentBuild",
    ":apps:desktop:resolveAndVerifyDependenciesForCurrentBuild",
    ":network:resolveAndVerifyDependenciesForCurrentBuild",
    ":network:tor:resolveAndVerifyDependenciesForCurrentBuild",
)

fun htmlEscape(value: Any?): String =
    value?.toString()
        ?.replace("&", "&amp;")
        ?.replace("<", "&lt;")
        ?.replace(">", "&gt;")
        ?.replace("|", "&#124;")
        ?: ""

fun keyIdSuffix(keyId: String): String {
    val normalized = keyId.uppercase(Locale.ROOT).replace("\\s".toRegex(), "")
    return if (normalized.length > 16) normalized.substring(normalized.length - 16) else normalized
}

fun decodeArmoredPublicKey(armoredPublicKey: String): ByteArray {
    val base64Lines = mutableListOf<String>()
    var inBody = false
    armoredPublicKey.lineSequence().forEach { line ->
        when {
            line.startsWith("-----BEGIN") -> return@forEach
            !inBody && line.trim().isEmpty() -> {
                inBody = true
                return@forEach
            }
            !inBody -> return@forEach
            line.startsWith("=") || line.startsWith("-----END") -> return@forEach
            line.trim().isNotEmpty() -> base64Lines += line.trim()
        }
    }

    if (base64Lines.isEmpty()) {
        return ByteArray(0)
    }

    return try {
        Base64.getDecoder().decode(base64Lines.joinToString(""))
    } catch (_: Exception) {
        ByteArray(0)
    }
}

fun parseOpenPgpPackets(armoredPublicKey: String): List<OpenPgpPacket> {
    val packets = mutableListOf<OpenPgpPacket>()
    return try {
        val bytes = decodeArmoredPublicKey(armoredPublicKey)
        var offset = 0
        while (offset < bytes.size) {
            val ctb = bytes[offset++].toInt() and 0xff
            if ((ctb and 0x80) == 0) {
                return packets
            }

            val tag: Int
            val length: Long
            if ((ctb and 0x40) != 0) {
                tag = ctb and 0x3f
                val firstLengthOctet = bytes[offset++].toInt() and 0xff
                length = when {
                    firstLengthOctet < 192 -> firstLengthOctet.toLong()
                    firstLengthOctet <= 223 ->
                        (((firstLengthOctet - 192) shl 8) + (bytes[offset++].toInt() and 0xff) + 192).toLong()
                    firstLengthOctet == 255 ->
                        ((bytes[offset++].toLong() and 0xffL) shl 24) or
                                ((bytes[offset++].toLong() and 0xffL) shl 16) or
                                ((bytes[offset++].toLong() and 0xffL) shl 8) or
                                (bytes[offset++].toLong() and 0xffL)
                    else -> return packets
                }
            } else {
                tag = (ctb shr 2) and 0x0f
                val lengthType = ctb and 0x03
                length = when (lengthType) {
                    0 -> (bytes[offset++].toInt() and 0xff).toLong()
                    1 -> (((bytes[offset++].toInt() and 0xff) shl 8) or (bytes[offset++].toInt() and 0xff)).toLong()
                    2 ->
                        ((bytes[offset++].toLong() and 0xffL) shl 24) or
                                ((bytes[offset++].toLong() and 0xffL) shl 16) or
                                ((bytes[offset++].toLong() and 0xffL) shl 8) or
                                (bytes[offset++].toLong() and 0xffL)
                    else -> (bytes.size - offset).toLong()
                }
            }

            if (length > Int.MAX_VALUE || offset + length.toInt() > bytes.size) {
                return packets
            }

            packets += OpenPgpPacket(tag, bytes.copyOfRange(offset, offset + length.toInt()))
            offset += length.toInt()
        }
        packets
    } catch (_: Exception) {
        packets
    }
}

fun normalizeOpenPgpUserId(userId: String): String =
    userId
        .replace("\uFFFDamonn McManus <eamonn@mcmanus.net>", "\u00C9amonn McManus <eamonn@mcmanus.net>")
        .replace("\u003Famonn McManus <eamonn@mcmanus.net>", "\u00C9amonn McManus <eamonn@mcmanus.net>")

fun parsePublicKeyUserIds(armoredPublicKey: String): List<String> =
    parseOpenPgpPackets(armoredPublicKey)
        .filter { it.tag == 13 }
        .map { normalizeOpenPgpUserId(String(it.body, StandardCharsets.UTF_8).trim()) }
        .filter { it.isNotEmpty() }

fun parsePublicKeyCreatedDate(armoredPublicKey: String): String? {
    val publicKeyPacket = parseOpenPgpPackets(armoredPublicKey).find { it.tag == 6 } ?: return null
    if (publicKeyPacket.body.size < 5) {
        return null
    }

    val body = publicKeyPacket.body
    val created = ((body[1].toLong() and 0xffL) shl 24) or
            ((body[2].toLong() and 0xffL) shl 16) or
            ((body[3].toLong() and 0xffL) shl 8) or
            (body[4].toLong() and 0xffL)

    return Instant.ofEpochSecond(created)
        .atZone(ZoneOffset.UTC)
        .toLocalDate()
        .toString()
}

fun parseSignerUserId(userId: String): Triple<String, String, String> {
    val matcher = Regex("^(.*?)\\s*<([^>]+)>$").matchEntire(userId)
    return if (matcher != null) {
        Triple(matcher.groupValues[1].trim(), matcher.groupValues[2].trim(), userId)
    } else {
        Triple(userId, "", userId)
    }
}

fun loadSignerMetadata(keyringFile: File): Map<String, SignerMetadata> {
    if (!keyringFile.exists()) {
        return emptyMap()
    }

    val signers = mutableMapOf<String, SignerMetadata>()
    data class CurrentKey(
        val keyId: String,
        val subKeyIds: MutableList<String> = mutableListOf(),
        val userIds: MutableList<String> = mutableListOf(),
        val armor: StringBuilder = StringBuilder(),
        var inArmor: Boolean = false,
    )

    var current: CurrentKey? = null
    fun saveCurrent() {
        val key = current ?: return
        val userIds = parsePublicKeyUserIds(key.armor.toString()).ifEmpty { key.userIds }
        val parsedUser = userIds.firstOrNull()?.let { parseSignerUserId(it) } ?: Triple("", "", "")
        val signer = SignerMetadata(
            keyId = key.keyId,
            name = parsedUser.first,
            email = parsedUser.second,
            userId = parsedUser.third,
            created = parsePublicKeyCreatedDate(key.armor.toString()) ?: "",
        )
        (listOf(key.keyId) + key.subKeyIds).forEach { keyId ->
            signers[keyId] = signer
        }
    }

    keyringFile.forEachLine(StandardCharsets.UTF_8) { line ->
        val pubMatcher = Regex("^pub\\s+([0-9A-Fa-f]+)\\s*$").matchEntire(line)
        if (pubMatcher != null) {
            saveCurrent()
            current = CurrentKey(keyIdSuffix(pubMatcher.groupValues[1]))
            return@forEachLine
        }

        val key = current ?: return@forEachLine
        val uidMatcher = Regex("^uid\\s+(.*)$").matchEntire(line)
        if (uidMatcher != null) {
            key.userIds += uidMatcher.groupValues[1].trim()
            return@forEachLine
        }

        val subMatcher = Regex("^sub\\s+([0-9A-Fa-f]+)\\s*$").matchEntire(line)
        if (subMatcher != null) {
            key.subKeyIds += keyIdSuffix(subMatcher.groupValues[1])
            return@forEachLine
        }

        if (line.startsWith("-----BEGIN PGP PUBLIC KEY BLOCK-----")) {
            key.inArmor = true
        }
        if (key.inArmor) {
            key.armor.append(line).append('\n')
        }
        if (line.startsWith("-----END PGP PUBLIC KEY BLOCK-----")) {
            key.inArmor = false
        }
    }
    saveCurrent()

    return signers
}

fun resolvableConfigurations(): List<ResolvableConfiguration> =
    rootProject.allprojects
        .flatMap { gradleProject ->
            (gradleProject.configurations.toList() + gradleProject.buildscript.configurations.toList())
                .filter { it.isCanBeResolved }
                .map { ResolvableConfiguration(gradleProject.path, it) }
        }
        .sortedBy { "${it.projectPath}:${it.configuration.name}" }

fun resolveConfigurations(collectComponents: Boolean): ResolvedBuildDependencies {
    val resolvedComponents = TreeSet<String>()
    val directComponents = TreeSet<String>()
    val artifactsByComponent = TreeMap<String, MutableSet<String>>()
    val failures = mutableListOf<String>()
    val configurations = resolvableConfigurations()

    configurations.forEach { entry ->
        val configuration = entry.configuration
        try {
            if (collectComponents) {
                val result = configuration.incoming.resolutionResult
                result.root.dependencies.forEach { dependency ->
                    val selected = (dependency as? ResolvedDependencyResult)?.selected?.id
                    if (selected is ModuleComponentIdentifier) {
                        directComponents += "${selected.group}:${selected.module}:${selected.version}"
                    }
                }
                result.allComponents.forEach { component ->
                    val id = component.id
                    if (id is ModuleComponentIdentifier) {
                        resolvedComponents += "${id.group}:${id.module}:${id.version}"
                    }
                }
                configuration.incoming.artifacts.artifacts.forEach { artifact ->
                    val id = artifact.id.componentIdentifier
                    if (id is ModuleComponentIdentifier) {
                        artifactsByComponent
                            .getOrPut("${id.group}:${id.module}:${id.version}") { TreeSet() }
                            .add(artifact.file.name)
                    }
                }
            }

            configuration.resolve()
        } catch (exception: Exception) {
            failures += "${entry.projectPath}:${configuration.name} - ${exception.message}"
        }
    }

    if (failures.isNotEmpty()) {
        throw GradleException("Failed to resolve ${failures.size} configuration(s):\n - ${failures.joinToString("\n - ")}")
    }

    return ResolvedBuildDependencies(
        resolvedComponents = resolvedComponents,
        directComponents = directComponents,
        configurationCount = configurations.size,
        artifactsByComponent = artifactsByComponent.mapValues { it.value.toSet() },
    )
}

fun dependencyVerificationInventoryFile(): File {
    val relativeBuildDir = bisqRepositoryRoot.toPath().relativize(rootDir.canonicalFile.toPath()).toString()
    val buildId = if (relativeBuildDir.isEmpty()) {
        "root"
    } else {
        relativeBuildDir.replace(File.separatorChar, '_')
    }
    return File(dependencyVerificationInventoryDir, "$buildId.tsv")
}

fun writeResolvedDependencyInventory(resolved: ResolvedBuildDependencies) {
    val inventoryFile = dependencyVerificationInventoryFile()
    inventoryFile.parentFile.mkdirs()
    val lines = mutableListOf("configurations\t${resolved.configurationCount}")
    resolved.resolvedComponents.forEach { id ->
        val scope = if (resolved.directComponents.contains(id)) "direct" else "transitive"
        lines += "$scope\t$id"
    }
    resolved.artifactsByComponent.forEach { (id, artifactNames) ->
        artifactNames.forEach { artifactName ->
            lines += "artifact\t$id\t$artifactName"
        }
    }
    inventoryFile.writeText(lines.joinToString(System.lineSeparator()) + System.lineSeparator(), StandardCharsets.UTF_8)
}

fun readResolvedDependencyInventory(): ResolvedBuildDependencies {
    val resolvedComponents = TreeSet<String>()
    val directComponents = TreeSet<String>()
    val artifactsByComponent = TreeMap<String, MutableSet<String>>()
    var configurationCount = 0
    if (!dependencyVerificationInventoryDir.isDirectory) {
        return ResolvedBuildDependencies(emptySet(), emptySet(), 0)
    }

    dependencyVerificationInventoryDir.listFiles { file -> file.isFile && file.extension == "tsv" }
        ?.sortedBy { it.name }
        ?.forEach { file ->
            file.forEachLine(StandardCharsets.UTF_8) { line ->
                val columns = line.split('\t')
                when (columns[0]) {
                    "configurations" -> if (columns.size == 2) {
                        configurationCount += columns[1].toInt()
                    }
                    "direct" -> {
                        if (columns.size == 2) {
                            resolvedComponents += columns[1]
                            directComponents += columns[1]
                        }
                    }
                    "transitive" -> if (columns.size == 2) {
                        resolvedComponents += columns[1]
                    }
                    "artifact" -> if (columns.size == 3) {
                        artifactsByComponent.getOrPut(columns[1]) { TreeSet() }.add(columns[2])
                    }
                }
            }
        }

    return ResolvedBuildDependencies(
        resolvedComponents = resolvedComponents,
        directComponents = directComponents,
        configurationCount = configurationCount,
        artifactsByComponent = artifactsByComponent.mapValues { it.value.toSet() },
    )
}

fun readChecksumFallbackAllowlist(allowlistFile: File): ChecksumFallbackAllowlist {
    if (!allowlistFile.exists()) {
        throw GradleException("Missing $allowlistFile. Review checksum fallback dependencies and add the approved entries.")
    }

    fun sortKey(entry: String): String {
        val columns = entry.split('\t')
        val module = columns[0].split(':')
        return "${module[0]}\t${module[1]}\t${module[2]}\t${columns[1]}"
    }

    val allowedEntries = TreeSet<String>()
    val rationaleByEntry = TreeMap<String, String>()
    val entryOrder = mutableListOf<String>()
    val duplicateEntries = mutableListOf<String>()
    allowlistFile.readLines(StandardCharsets.UTF_8).forEachIndexed { index, line ->
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return@forEachIndexed
        }

        val columns = line.split('\t')
        if (columns.size != 3 || columns.any { it.trim().isEmpty() }) {
            throw GradleException(
                "Invalid checksum fallback allowlist entry at $allowlistFile:${index + 1}. " +
                        "Expected '<group:name:version>\\t<artifact-file-name>\\t<review-rationale>'."
            )
        }
        val coordinate = columns[0].trim()
        val coordinateParts = coordinate.split(':')
        if (coordinateParts.size != 3 || coordinateParts.any { it.isEmpty() }) {
            throw GradleException(
                "Invalid checksum fallback allowlist coordinate at $allowlistFile:${index + 1}: '$coordinate'. " +
                        "Expected '<group:name:version>' in entry '$line'."
            )
        }

        val entry = "$coordinate\t${columns[1].trim()}"
        entryOrder += entry
        if (!allowedEntries.add(entry)) {
            duplicateEntries += entry
        }
        rationaleByEntry[entry] = columns[2].trim()
    }

    if (duplicateEntries.isNotEmpty()) {
        throw GradleException("Duplicate checksum fallback allowlist entries:\n - ${duplicateEntries.distinct().sorted().joinToString("\n - ")}")
    }
    if (entryOrder != entryOrder.sortedBy { sortKey(it) }) {
        throw GradleException("Checksum fallback allowlist entries must be sorted by module and artifact:\n$allowlistFile")
    }

    return ChecksumFallbackAllowlist(allowedEntries, rationaleByEntry)
}

fun NodeList.elements(): List<Element> =
    (0 until length).mapNotNull { item(it) as? Element }

fun parseVerificationMetadata(metadataFile: File): Pair<Map<String, VerifiedComponent>, List<TrustedKey>> {
    val document = DocumentBuilderFactory.newInstance()
        .newDocumentBuilder()
        .parse(metadataFile)

    val componentsById = TreeMap<String, VerifiedComponent>()
    document.getElementsByTagName("component").elements().forEach { component ->
        val group = component.getAttribute("group")
        val name = component.getAttribute("name")
        val version = component.getAttribute("version")
        val id = "$group:$name:$version"
        val artifacts = component.getElementsByTagName("artifact").elements().map { artifact ->
            val checksumFallbackReason = artifact.getElementsByTagName("sha256").elements()
                .map { it.getAttribute("reason") }
                .firstOrNull { it.isNotEmpty() }
                ?: ""
            VerifiedArtifact(
                name = artifact.getAttribute("name"),
                checksumOnly = checksumFallbackReason.isNotEmpty(),
                checksumFallbackReason = checksumFallbackReason,
                keyIds = artifact.getElementsByTagName("pgp").elements()
                    .map { it.getAttribute("value") }
                    .filter { it.isNotEmpty() },
            )
        }
        componentsById[id] = VerifiedComponent(id, group, name, version, artifacts)
    }

    val trustedKeys = document.getElementsByTagName("trusted-key").elements().map { trustedKey ->
        val entries = mutableListOf<TrustedKeyEntry>()
        if (
            trustedKey.getAttribute("group").isNotEmpty() ||
            trustedKey.getAttribute("name").isNotEmpty() ||
            trustedKey.getAttribute("version").isNotEmpty()
        ) {
            entries += TrustedKeyEntry(
                group = trustedKey.getAttribute("group"),
                name = trustedKey.getAttribute("name"),
                version = trustedKey.getAttribute("version"),
                regex = trustedKey.getAttribute("regex") == "true",
            )
        }
        trustedKey.getElementsByTagName("trusting").elements().forEach { trusting ->
            entries += TrustedKeyEntry(
                group = trusting.getAttribute("group"),
                name = trusting.getAttribute("name"),
                version = trusting.getAttribute("version"),
                regex = trusting.getAttribute("regex") == "true",
            )
        }
        TrustedKey(trustedKey.getAttribute("id"), entries)
    }

    return componentsById to trustedKeys
}

fun patternMatches(pattern: String, value: String, regex: Boolean): Boolean =
    pattern.isEmpty() || if (regex) Regex(pattern).matches(value) else pattern == value

val resolveCurrentBuildTask = tasks.register("resolveAndVerifyDependenciesForCurrentBuild") {
    group = "verification"
    description = "Resolves every resolvable configuration in this Gradle build."

    outputs.file(dependencyVerificationInventoryFile())
    outputs.upToDateWhen { false }

    doLast {
        val resolved = resolveConfigurations(true)
        writeResolvedDependencyInventory(resolved)
        logger.lifecycle("Resolved and verified ${resolved.configurationCount} configurations in ${rootProject.name}.")
    }
}

if (isBisqRepositoryRootBuild) {
    tasks.register("resolveAndVerifyDependencies") {
        group = "verification"
        description = "Resolves every resolvable configuration in the Bisq 2 composite build."

        doLast {
            dependencyVerificationInventoryDir.deleteRecursively()

            val currentBuildResolved = resolveConfigurations(true)
            writeResolvedDependencyInventory(currentBuildResolved)
            logger.lifecycle("Resolved and verified ${currentBuildResolved.configurationCount} configurations in ${rootProject.name}.")

            val writeVerificationArguments = gradle.startParameter.writeDependencyVerifications
                .takeIf { it.isNotEmpty() }
                ?.let { listOf("--write-verification-metadata", it.joinToString(",")) }
                ?: emptyList()
            val passthroughArguments = buildList {
                add("--no-daemon")
                add("--no-parallel")
                if (gradle.startParameter.isOffline) {
                    add("--offline")
                }
                if (gradle.startParameter.isRefreshDependencies) {
                    add("--refresh-dependencies")
                }
                addAll(writeVerificationArguments)
            }

            includedBuildCurrentResolverTaskPaths.forEach { taskPath ->
                val result = project.exec {
                    workingDir = bisqRepositoryRoot
                    commandLine(listOf(gradleWrapperCommand) + passthroughArguments + taskPath)
                    isIgnoreExitValue = true
                }
                if (result.exitValue != 0) {
                    throw GradleException("Failed to run $gradleWrapperCommand ${passthroughArguments.joinToString(" ")} $taskPath")
                }
            }

            val resolved = readResolvedDependencyInventory()
            logger.lifecycle("Resolved and verified ${resolved.configurationCount} configurations across the Bisq 2 composite build.")
        }
    }

    tasks.register("refreshDependencyVerificationKeyring") {
        group = "verification"
        description = "Refreshes dependency verification PGP public keys from key servers and exports the armored keyring."

        inputs.file(dependencyVerificationMetadata)
        outputs.file(dependencyVerificationKeyring)
        outputs.upToDateWhen { false }

        doLast {
            val previousModified = if (dependencyVerificationKeyring.exists()) dependencyVerificationKeyring.lastModified() else 0L
            val result = project.exec {
                workingDir = bisqRepositoryRoot
                commandLine(gradleWrapperCommand, "--no-daemon", "help", "--refresh-keys", "--export-keys")
                isIgnoreExitValue = true
            }

            if (!dependencyVerificationKeyring.exists()) {
                throw GradleException(
                    "Failed to write $dependencyVerificationKeyring. " +
                            "Check network access to the configured Gradle verification key servers."
                )
            }

            if (result.exitValue != 0) {
                if (dependencyVerificationKeyring.lastModified() <= previousModified) {
                    throw GradleException(
                        "Gradle --refresh-keys --export-keys failed with exit code ${result.exitValue} " +
                                "and did not update $dependencyVerificationKeyring."
                    )
                }
                logger.warn(
                    "Gradle --refresh-keys --export-keys exited with ${result.exitValue} after updating " +
                            "$dependencyVerificationKeyring. This can happen in some Gradle/Bouncy Castle combinations."
                )
            }

            if (dependencyVerificationBinaryKeyring.exists()) {
                logger.warn("Ignoring $dependencyVerificationBinaryKeyring; dependency verification is configured to use the armored keyring.")
            }

            logger.lifecycle("Refreshed $dependencyVerificationKeyring.")
        }
    }

    tasks.register("verifyDependencySignaturePolicy") {
        group = "verification"
        description = "Verifies all dependency artifacts and fails if checksum fallback artifacts are not explicitly allowed."

        dependsOn("resolveAndVerifyDependencies")
        inputs.file(dependencyVerificationMetadata)
        inputs.file(dependencyChecksumFallbackAllowlist)

        doLast {
            if (!dependencyVerificationMetadata.exists()) {
                throw GradleException(
                    "Missing $dependencyVerificationMetadata. " +
                            "Run './gradlew resolveAndVerifyDependencies --write-verification-metadata pgp,sha256' first."
                )
            }
            val allowlist = readChecksumFallbackAllowlist(dependencyChecksumFallbackAllowlist)
            val (componentsById) = parseVerificationMetadata(dependencyVerificationMetadata)
            val checksumFallbackEntries = TreeSet<String>()
            componentsById.values.forEach { component ->
                component.artifacts.filter { it.checksumOnly }.forEach { artifact ->
                    checksumFallbackEntries += "${component.id}\t${artifact.name}"
                }
            }

            val unapprovedEntries = checksumFallbackEntries.filter { !allowlist.entries.contains(it) }
            val staleEntries = allowlist.entries.filter { !checksumFallbackEntries.contains(it) }
            if (unapprovedEntries.isNotEmpty() || staleEntries.isNotEmpty()) {
                val message = StringBuilder("Dependency signature policy failed.")
                if (unapprovedEntries.isNotEmpty()) {
                    message.append("\n\nUnapproved checksum fallback dependency artifacts:\n - ")
                        .append(unapprovedEntries.joinToString("\n - "))
                }
                if (staleEntries.isNotEmpty()) {
                    message.append("\n\nAllowlist entries that are no longer checksum fallback artifacts:\n - ")
                        .append(staleEntries.joinToString("\n - "))
                }
                message.append("\n\nReview each checksum fallback artifact, update $dependencyChecksumFallbackAllowlist, and regenerate $dependencySignatureReport.")
                throw GradleException(message.toString())
            }

            logger.lifecycle("Verified ${checksumFallbackEntries.size} approved checksum fallback dependency artifact(s).")
        }
    }

    tasks.register("dependencySignatureReport") {
        group = "verification"
        description = "Writes a report of signed and checksum fallback dependencies from Gradle verification metadata."

        dependsOn("resolveAndVerifyDependencies")
        inputs.file(dependencyVerificationMetadata)
        inputs.file(dependencyVerificationKeyring).optional()
        inputs.file(dependencyChecksumFallbackAllowlist)
        outputs.file(dependencySignatureReport)
        outputs.upToDateWhen { false }

        doLast {
            if (!dependencyVerificationMetadata.exists()) {
                throw GradleException(
                    "Missing $dependencyVerificationMetadata. " +
                            "Run './gradlew resolveAndVerifyDependencies --write-verification-metadata pgp,sha256' first."
                )
            }

            val resolved = readResolvedDependencyInventory()
            val allowlist = readChecksumFallbackAllowlist(dependencyChecksumFallbackAllowlist)
            val signerMetadata = loadSignerMetadata(dependencyVerificationKeyring)
            val (componentsById, trustedKeys) = parseVerificationMetadata(dependencyVerificationMetadata)

            fun matchingTrustedKeyIds(group: String, name: String, version: String): List<String> =
                trustedKeys
                    .filter { trustedKey ->
                        trustedKey.entries.any { entry ->
                            patternMatches(entry.group, group, entry.regex) &&
                                    patternMatches(entry.name, name, entry.regex) &&
                                    patternMatches(entry.version, version, entry.regex)
                        }
                    }
                    .map { it.id }
                    .distinct()
                    .sorted()

            val rows = resolved.resolvedComponents.map { id ->
                val component = componentsById[id]
                if (component == null) {
                    val (group, name, version) = id.split(':', limit = 3)
                    val matchingTrustedKeys = matchingTrustedKeyIds(group, name, version)
                    if (matchingTrustedKeys.isEmpty()) {
                        DependencyReportRow(
                            id = id,
                            scope = if (resolved.directComponents.contains(id)) "direct" else "transitive",
                            status = "missing from metadata",
                            signedCount = 0,
                            checksumOnlyCount = 0,
                            keyIds = emptyList(),
                            checksumArtifacts = listOf("metadata entry missing" to ""),
                        )
                    } else {
                        DependencyReportRow(
                            id = id,
                            scope = if (resolved.directComponents.contains(id)) "direct" else "transitive",
                            status = "PGP signed",
                            signedCount = resolved.artifactsByComponent[id]?.size ?: 0,
                            checksumOnlyCount = 0,
                            keyIds = matchingTrustedKeys,
                            checksumArtifacts = emptyList(),
                        )
                    }
                } else {
                    val matchingTrustedKeys = matchingTrustedKeyIds(component.group, component.name, component.version)
                    val signedCount = component.artifacts.count { !it.checksumOnly }
                    val checksumOnlyCount = component.artifacts.count { it.checksumOnly }
                    val status = if (checksumOnlyCount == 0) {
                        "PGP signed"
                    } else if (signedCount == 0) {
                        "checksum fallback"
                    } else {
                        "mixed"
                    }
                    val keyIds = if (signedCount == 0) {
                        emptyList()
                    } else {
                        (component.artifacts.flatMap { it.keyIds } + matchingTrustedKeys).distinct().sorted()
                    }
                    DependencyReportRow(
                        id = id,
                        scope = if (resolved.directComponents.contains(id)) "direct" else "transitive",
                        status = status,
                        signedCount = signedCount,
                        checksumOnlyCount = checksumOnlyCount,
                        keyIds = keyIds,
                        checksumArtifacts = component.artifacts
                            .filter { it.checksumOnly }
                            .map { artifact ->
                                artifact.name to (allowlist.rationales["${component.id}\t${artifact.name}"] ?: "Missing allowlist rationale")
                            }
                            .sortedBy { it.first },
                    )
                }
            }

            val signedRows = rows.filter { it.status == "PGP signed" }
            val checksumRows = rows.filter { it.status == "checksum fallback" }
            val mixedRows = rows.filter { it.status == "mixed" }
            val missingRows = rows.filter { it.status == "missing from metadata" }
            val totalArtifacts = rows.sumOf { it.signedCount + it.checksumOnlyCount }
            val signedArtifacts = rows.sumOf { it.signedCount }
            val checksumOnlyArtifacts = rows.sumOf { it.checksumOnlyCount }
            val reportedKeyIds = rows.flatMap { it.keyIds }.distinct().sorted()
            val reportedKeyIdsInKeyring = reportedKeyIds.count { signerMetadata[keyIdSuffix(it)] != null }
            val reportedKeyIdsWithUserId = reportedKeyIds.count { keyId ->
                val signer = signerMetadata[keyIdSuffix(keyId)]
                signer?.name?.isNotEmpty() == true || signer?.email?.isNotEmpty() == true
            }
            val reportedKeyIdsWithCreatedDate = reportedKeyIds.count { keyId ->
                signerMetadata[keyIdSuffix(keyId)]?.created?.isNotEmpty() == true
            }

            fun signerDetails(keyIds: List<String>): String {
                if (keyIds.isEmpty()) {
                    return "-"
                }

                return keyIds.joinToString("<br><br>") { keyId ->
                    val signer = signerMetadata[keyIdSuffix(keyId)]
                    if (signer == null) {
                        "`$keyId`"
                    } else {
                        buildList {
                            add("`$keyId`")
                            if (signer.name.isNotEmpty()) {
                                add(htmlEscape(signer.name))
                            }
                            if (signer.email.isNotEmpty()) {
                                add("`${signer.email}`")
                            }
                            if (signer.created.isNotEmpty()) {
                                add("created ${signer.created}")
                            }
                        }.joinToString("<br>")
                    }
                }
            }

            dependencySignatureReport.parentFile.mkdirs()
            val markdown = StringBuilder()
            markdown.append("# Dependency Signature Report\n\n")
            markdown.append("Generated from `gradle/verification-metadata.xml` after resolving ${resolved.configurationCount} configurations.\n\n")
            if (dependencyVerificationKeyring.exists()) {
                markdown.append("Signer metadata is loaded from `gradle/verification-keyring.keys`; names and emails come from the first OpenPGP user ID, and creation dates come from the public key packet.\n\n")
            } else {
                markdown.append("Signer metadata is unavailable because `gradle/verification-keyring.keys` is missing.\n\n")
            }
            markdown.append("Checksum fallback review rationales are loaded from `gradle/dependency-checksum-fallback-allowlist.tsv`.\n\n")
            markdown.append("Refresh the metadata before regenerating this report:\n\n")
            markdown.append("```bash\n")
            markdown.append("./gradlew refreshDependencyVerificationKeyring\n")
            markdown.append("./gradlew resolveAndVerifyDependencies --write-verification-metadata pgp,sha256\n")
            markdown.append("./gradlew verifyDependencySignaturePolicy\n")
            markdown.append("./gradlew dependencySignatureReport\n")
            markdown.append("```\n\n")
            markdown.append("## Summary\n\n")
            markdown.append("| Metric | Count |\n")
            markdown.append("| --- | ---: |\n")
            markdown.append("| Resolved external modules | ${rows.size} |\n")
            markdown.append("| Modules with PGP-signed artifacts only | ${signedRows.size} |\n")
            markdown.append("| Modules using checksum fallback only | ${checksumRows.size} |\n")
            markdown.append("| Modules with mixed signed/checksum-fallback artifacts | ${mixedRows.size} |\n")
            markdown.append("| Modules missing verification metadata | ${missingRows.size} |\n")
            markdown.append("| Verified artifacts | $totalArtifacts |\n")
            markdown.append("| PGP-signed artifacts | $signedArtifacts |\n")
            markdown.append("| Checksum-fallback artifacts | $checksumOnlyArtifacts |\n")
            markdown.append("| Signer keys found in exported keyring | $reportedKeyIdsInKeyring / ${reportedKeyIds.size} |\n")
            markdown.append("| Signer keys with name or email | $reportedKeyIdsWithUserId / ${reportedKeyIds.size} |\n")
            markdown.append("| Signer keys with creation date | $reportedKeyIdsWithCreatedDate / ${reportedKeyIds.size} |\n\n")
            markdown.append("## Handling Transitive Dependencies\n\n")
            markdown.append("Treat transitive dependencies the same as direct dependencies. Gradle verifies the resolved artifact graph, so a transitive artifact with a published signature should be PGP-verified, and an artifact Gradle cannot verify by signature should keep an explicit SHA-256 checksum fallback. Review metadata changes separately when dependency versions change, especially new trusted keys and new checksum fallback artifacts.\n\n")
            markdown.append("## Checksum Fallback Dependencies\n\n")
            if (checksumRows.isEmpty() && mixedRows.isEmpty()) {
                markdown.append("All resolved modules have PGP-signed artifacts in the current metadata.\n\n")
            } else {
                markdown.append("| Dependency | Scope | Checksum fallback artifacts and review rationale |\n")
                markdown.append("| --- | --- | --- |\n")
                (checksumRows + mixedRows).sortedBy { it.id }.forEach { row ->
                    val checksumArtifacts = row.checksumArtifacts.joinToString("<br><br>") { artifact ->
                        "`${artifact.first}`<br>${htmlEscape(artifact.second)}"
                    }
                    markdown.append("| `${row.id}` | ${row.scope} | $checksumArtifacts |\n")
                }
                markdown.append('\n')
            }

            if (missingRows.isNotEmpty()) {
                markdown.append("## Missing Metadata\n\n")
                markdown.append("| Dependency | Scope |\n")
                markdown.append("| --- | --- |\n")
                missingRows.sortedBy { it.id }.forEach { row ->
                    markdown.append("| `${row.id}` | ${row.scope} |\n")
                }
                markdown.append('\n')
            }

            markdown.append("## Full Resolved Dependency Inventory\n\n")
            markdown.append("| Dependency | Scope | Status | Artifacts | Signer key and metadata |\n")
            markdown.append("| --- | --- | --- | ---: | --- |\n")
            rows.sortedBy { it.id }.forEach { row ->
                val artifactSummary = "${row.signedCount} signed / ${row.checksumOnlyCount} checksum"
                markdown.append("| `${row.id}` | ${row.scope} | ${row.status} | $artifactSummary | ${signerDetails(row.keyIds)} |\n")
            }

            dependencySignatureReport.writeText(markdown.toString(), StandardCharsets.UTF_8)
            logger.lifecycle("Wrote $dependencySignatureReport.")
        }
    }
} else {
    tasks.register("resolveAndVerifyDependencies") {
        group = "verification"
        description = "Resolves every resolvable configuration in this Gradle build."
        dependsOn(resolveCurrentBuildTask)
    }
}
