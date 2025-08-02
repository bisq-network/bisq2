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

include("http-api-app")
include("seed-node-app")
include("oracle-node-app")
include("node-monitor-web-app")
include("resilience-test-app")

rootProject.name = "apps"
