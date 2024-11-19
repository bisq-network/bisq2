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
includeBuild("desktop")

include("rest-api-app")
include("seed-node-app")
include("oracle-node-app")
include("node-monitor-web-app")

rootProject.name = "apps"
