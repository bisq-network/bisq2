dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

include("commons")
include("gradle-tasks")
include("packaging")
include("maven-publisher")
include("tor-binary")
include("toolchain-resolver")
include("webcam-app")
