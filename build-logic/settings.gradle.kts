dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

includeBuild("../wallets/bitcoind/build-logic") {
    name = "bitcoind-build-logic"
}

include("commons")
include("desktop-regtest")
include("electrum-binaries")
include("packaging")
include("tor-binary")
include("toolchain-resolver")
include("webcam-app")
