dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

include("commons")
include("bitcoin-core-binaries")
include("desktop-regtest")
include("gradle-tasks")
include("electrum-binaries")
include("packaging")
include("tor-binary")
include("toolchain-resolver")
include("webcam-app")
