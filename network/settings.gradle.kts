pluginManagement {
    repositories {
        gradlePluginPortal()
    }
    includeBuild("../build-logic")
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

includeBuild("..")
includeBuild("tor")

include("network-common")
include("i2p")
include("network")
include("network-identity")
include("socks5-socket-channel")

rootProject.name = "network"
