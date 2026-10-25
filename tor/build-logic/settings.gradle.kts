dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../../gradle/libs.versions.toml"))
        }
    }
}

include("commons")
include("gradle-tasks")
include("maven-publisher")
include("tor-binary")
include("toolchain-resolver")
