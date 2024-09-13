pluginManagement {
    repositories {
        gradlePluginPortal()
    }
    includeBuild("build-logic")
    includeBuild("../../build-logic") {
        name = "root-build-logic"
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../../gradle/libs.versions.toml"))
        }
    }
}

include("core")
include("bitcoind")
include("json-rpc")
include("regtest")

rootProject.name = "bitcoind"