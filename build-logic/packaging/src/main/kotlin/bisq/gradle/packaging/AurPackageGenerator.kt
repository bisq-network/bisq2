package bisq.gradle.packaging

import org.apache.commons.codec.binary.Hex
import java.io.File
import java.nio.file.Path
import java.security.MessageDigest

class AurPackageGenerator {

    fun generateAurPackage(
        appName: String,
        appVersion: String,
        outputDir: Path,
        maintainers: List<Pair<String, String>> = listOf(
            "David Parrish" to "daveparrish@tutanota.com",
            "Felix Golatofski" to "contact@xdfr.de"
        ),
        categories: String = "Network;Finance;"
    ) {
        val platformName = getPlatform().platformName
        val aurPackageDirName = "Bisq-$appVersion-$platformName-aur"
        val aurPackageDir = outputDir.resolve(aurPackageDirName).toFile()
        aurPackageDir.mkdirs()

        val desktopFile = generateDesktopFile(aurPackageDir, appName, categories)
        val desktopFileChecksum = calculateSha256(desktopFile)

        generatePkgbuild(aurPackageDir, appName, appVersion, desktopFileChecksum, maintainers)
        generateSrcInfo(aurPackageDir, appName, appVersion, desktopFileChecksum)
    }

    private fun generateDesktopFile(
        aurPackageDir: File,
        appName: String,
        categories: String
    ): File {
        val pkgName = getPkgName(appName)
        val binName = getBinName(appName)

        val desktopContent = """
            [Desktop Entry]
            Name=$binName
            GenericName=Bisq2
            Comment=The decentralized bitcoin exchange network    
            Exec=/usr/bin/$pkgName-desktop
            Icon=$pkgName
            Terminal=false
            Type=Application
            Categories=$categories
            Keywords=bitcoin;exchange;p2p;
        """.trimIndent()

        val desktopFile = File(aurPackageDir, "$pkgName.desktop")
        desktopFile.writeText(desktopContent)
        return desktopFile
    }

    private fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val fileBytes = file.readBytes()
        val hashBytes = digest.digest(fileBytes)
        return Hex.encodeHexString(hashBytes)
    }

    private fun generatePkgbuild(
        aurPackageDir: File,
        appName: String,
        appVersion: String,
        desktopFileChecksum: String,
        maintainers: List<Pair<String, String>>
    ) {
        val pkgName = getPkgName(appName)
        val binName = getBinName(appName)
        val licenseType = "AGPL-3.0-or-later"

        val maintainerLines = maintainers.mapIndexed { index, (name, email) ->
            if (index == 0) "# Maintainer: $name <$email>"
            else "# Co-Maintainer: $name <$email>"
        }.joinToString("\n")

        val pkgbuildContent = """
            |$maintainerLines
            |
            |pkgname=$pkgName
            |pkgver=$appVersion
            |pkgrel=1
            |pkgdesc="The Decentralized Trading Platform"
            |arch=('x86_64')
            |url="https://bisq.network"
            |license=('$licenseType')
            |depends=('java-runtime>=21' 'tor')
            |makedepends=('java-environment>=21' 'git')
            |source=("git+https://github.com/bisq-network/bisq2#tag=v${'$'}pkgver"
            |  "git+https://github.com/bisq-network/bitcoind.git"
            |  "$pkgName.desktop")
            |sha256sums=('SKIP'
            |  # SKIP is used for git sources as they are verified by git
            |  'SKIP'
            |  '$desktopFileChecksum')
            |_binname=$binName
            |provides=("$pkgName")
            |
            |prepare() {
            |  cd "${'$'}{srcdir}/${'$'}{pkgname}"
            |  git submodule init
            |  git config submodule.wallets/bitcoind.url "${'$'}srcdir/bitcoind"
            |  git -c protocol.file.allow=always submodule update
            |}
            |
            |build() {
            |  cd "${'$'}{srcdir}/${'$'}{pkgname}"
            |  msg2 "Building $pkgName..."
            |  ./gradlew --rerun-tasks apps:desktop:desktop-app:build
            |}
            |
            |check() {
            |  cd "${'$'}{srcdir}/${'$'}{pkgname}"
            |  msg2 "Testing $pkgName..."
            |  ./gradlew test
            |}
            |
            |package() {
            |  # Install executable.
            |  optdir="${'$'}{pkgdir}/opt/$pkgName"
            |  install -Dm644 "${'$'}{srcdir}/${'$'}{pkgname}/apps/desktop/desktop-app/build/libs/desktop-app-${'$'}pkgver-linux_x86_64-all.jar" "${'$'}optdir/lib/desktop-app-${'$'}pkgver-linux_x86_64-all.jar"
            |  install -Dm755 "${'$'}{srcdir}/${'$'}{pkgname}/apps/desktop/desktop-app/build/scriptsShadow/desktop-app" "${'$'}optdir/bin/$pkgName-desktop"
            |  install -d "${'$'}{pkgdir}/usr/bin"
            |  ln -s "/opt/$pkgName/bin/$pkgName-desktop" "${'$'}{pkgdir}/usr/bin/$pkgName-desktop"
            |  
            |  # Install desktop launcher.
            |  install -Dm644 $pkgName.desktop "${'$'}{pkgdir}/usr/share/applications/$pkgName.desktop"
            |  install -Dm644 "${'$'}{srcdir}/${'$'}{pkgname}/apps/desktop/desktop-app-launcher/package/linux/icon.png" "${'$'}{pkgdir}/usr/share/pixmaps/$pkgName.png"
            |}
            """.trimMargin()

        File(aurPackageDir, "PKGBUILD").writeText(pkgbuildContent)
    }

    private fun generateSrcInfo(
        aurPackageDir: File,
        appName: String,
        appVersion: String,
        desktopFileChecksum: String
    ) {
        val pkgName = getPkgName(appName)
        val licenseType = "AGPL-3.0-or-later"

        val srcInfoContent = """
            pkgbase=$pkgName
            pkgdesc=The Decentralized Trading Platform
            pkgver=$appVersion
            pkgrel=1
            url=https://bisq.network
            arch=x86_64
            license=$licenseType
            depends=java-runtime>=21
            depends=tor
            makedepends=java-environment>=21
            makedepends=git
            provides=$pkgName
            source=git+https://github.com/bisq-network/bisq2#tag=v${'$'}pkgver
            source=git+https://github.com/bisq-network/bitcoind.git
            source=$pkgName.desktop
            sha256sums=SKIP
            sha256sums=SKIP
            sha256sums=$desktopFileChecksum
            
            pkgname=$pkgName
        """.trimIndent()

        File(aurPackageDir, ".SRCINFO").writeText(srcInfoContent)
    }

    private fun getPkgName(appName: String): String {
        return appName.lowercase().replace(" ", "")
    }

    private fun getBinName(appName: String): String {
        return if (appName == "Bisq") "Bisq" else "Bisq2"
    }
}