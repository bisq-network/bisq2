dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

include("commons")
include("packaging")
include("maven-publisher")
include("toolchain-resolver")
include("webcam-app")
include("bi2p")
