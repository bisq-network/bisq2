pluginManagement {
    repositories {
        gradlePluginPortal()
    }
    includeBuild("../build-logic")
}

plugins {
    id("bisq.gradle.toolchain_resolver.ToolchainResolverPlugin")
}

toolchainManagement {
    jvm {
        javaRepositories {
            repository("bisq_zulu") {
                resolverClass.set(bisq.gradle.toolchain_resolver.BisqToolchainResolver::class.java)
            }
        }
    }
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

include("i2p")
include("network")
include("network-identity")
include("socks5-socket-channel")

rootProject.name = "network"
