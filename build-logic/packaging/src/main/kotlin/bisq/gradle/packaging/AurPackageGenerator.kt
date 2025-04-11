package bisq.gradle.packaging

import org.apache.commons.codec.binary.Hex
import java.io.File
import java.nio.file.Path
import java.security.MessageDigest

class AurPackageGenerator {

    fun generateAurPackage(
        appName: String,
        appVersion: String,
        outputDir: Path
    ) {
        val platformName = getPlatform().platformName
        val aurPackageDirName = "Bisq-$appVersion-$platformName-aur"
        val aurPackageDir = outputDir.resolve(aurPackageDirName).toFile()
        aurPackageDir.mkdirs()

        val desktopFile = generateDesktopFile(aurPackageDir, appName)
        val desktopFileChecksum = calculateSha256(desktopFile)

        generatePkgbuild(aurPackageDir, appName, appVersion, desktopFileChecksum)
        generateSrcInfo(aurPackageDir, appName, appVersion, desktopFileChecksum)
    }

    private fun generateDesktopFile(aurPackageDir: File, appName: String): File {
        val pkgName = appName.lowercase().replace(" ", "")
        val binName = if (appName == "Bisq") "Bisq" else "Bisq2"

        val desktopContent = """
            [Desktop Entry]
            Name=$binName
            GenericName=Bisq2
            Comment=The Decentralized Trading Platform    
            Exec=/usr/bin/$pkgName-desktop
            Icon=$pkgName
            Terminal=false
            Type=Application
            Categories=Utility;Finance;
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
        desktopFileChecksum: String
    ) {
        val pkgName = appName.lowercase().replace(" ", "")
        val binName = if (appName == "Bisq") "Bisq" else "Bisq2"
        val licenseType = "AGPL3"

        val pkgbuildContent = """
            # Maintainer: David Parrish <daveparrish@tutanota.com>
            # Co-Maintainer: Felix Golatofski <contact@xdfr.de>
            
            pkgname=$pkgName
            pkgver=$appVersion
            pkgrel=1
            pkgdesc="The Decentralized Trading Platform"
            arch=('x86_64')
            url="https://bisq.network"
            license=('$licenseType')
            depends=('java-runtime>=21' 'tor')
            makedepends=('java-environment>=21' 'git')
            source=("git+https://github.com/bisq-network/bisq2#tag=v${'$'}pkgver"
              "git+https://github.com/bisq-network/bitcoind.git"
              "$pkgName.desktop")
            sha256sums=('SKIP'
              'SKIP'
              '$desktopFileChecksum')
            _binname=$binName
            provides=("$pkgName")
            
            prepare() {
              cd "${'$'}{srcdir}/${'$'}{pkgname}"
              git submodule init
              git config submodule.wallets/bitcoind.url "${'$'}srcdir/bitcoind"
              git -c protocol.file.allow=always submodule update
            }
            
            build() {
              cd "${'$'}{srcdir}/${'$'}{pkgname}"
              msg2 "Building $pkgName..."
              ./gradlew --rerun-tasks apps:desktop:desktop-app:build
            }
            
            check() {
              cd "${'$'}{srcdir}/${'$'}{pkgname}"
              msg2 "Testing $pkgName..."
              ./gradlew test
            }
            
            package() {
              # Install executable.
              optdir="${'$'}{pkgdir}/opt/$pkgName"
              install -Dm644 "${'$'}{srcdir}/${'$'}{pkgname}/apps/desktop/desktop-app/build/libs/desktop-app-${'$'}pkgver-linux_x86_64-all.jar" "${'$'}{optdir}/lib/desktop-app-${'$'}pkgver-linux_x86_64-all.jar"
              install -Dm755 "${'$'}{srcdir}/${'$'}{pkgname}/apps/desktop/desktop-app/build/scriptsShadow/desktop-app" "${'$'}{optdir}/bin/$pkgName-desktop"
              install -d "${'$'}{pkgdir}/usr/bin"
              ln -s "/opt/$pkgName/bin/$pkgName-desktop" "${'$'}{pkgdir}/usr/bin/$pkgName-desktop"
              
              # Install desktop launcher.
              install -Dm644 $pkgName.desktop "${'$'}{pkgdir}/usr/share/applications/$pkgName.desktop"
              install -Dm644 "${'$'}{srcdir}/${'$'}{pkgname}/apps/desktop/desktop-app-launcher/package/linux/icon.png" "${'$'}{pkgdir}/usr/share/pixmaps/$pkgName.png"
            }
        """.trimIndent()

        File(aurPackageDir, "PKGBUILD").writeText(pkgbuildContent)
    }

    private fun generateSrcInfo(
        aurPackageDir: File,
        appName: String,
        appVersion: String,
        desktopFileChecksum: String
    ) {
        val pkgName = appName.lowercase().replace(" ", "")
        val licenseType = "AGPL3"

        val srcInfoContent = """
            pkgbase = $pkgName
            pkgdesc = The Decentralized Trading Platform
            pkgver = $appVersion
            pkgrel = 1
            url = https://bisq.network
            arch = x86_64
            license = $licenseType
            depends = java-runtime>=21
            depends = tor
            makedepends = java-environment>=21
            makedepends = git
            provides = $pkgName
            source = git+https://github.com/bisq-network/bisq2#tag=v${'$'}pkgver
            source = git+https://github.com/bisq-network/bitcoind.git
            source = $pkgName.desktop
            sha256sums = SKIP
            sha256sums = SKIP
            sha256sums = $desktopFileChecksum
            
            pkgname = $pkgName
        """.trimIndent()

        File(aurPackageDir, ".SRCINFO").writeText(srcInfoContent)
    }
}