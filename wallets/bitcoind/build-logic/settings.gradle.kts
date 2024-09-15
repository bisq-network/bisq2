dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../../../gradle/libs.versions.toml"))
        }
    }
}

include("bitcoin-core-binaries")
include("gradle-tasks")

rootProject.name = "bitcoind-build-logic"