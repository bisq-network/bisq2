pluginManagement {
    repositories {
        gradlePluginPortal()
    }
    includeBuild("build-logic")
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

rootProject.name = "bisq"

include("account")
include("application")
include("bisq-easy")
include("bonded-roles")
include("chat")
include("common")
include("desktop")
include("desktop-app")
include("desktop-app-launcher")
include("contract")
include("i2p")
include("identity")
include("i18n")
include("offer")
include("oracle-node")
include("oracle-node-app")
include("persistence")
include("platform")
include("presentation")
include("trade")
include("rest-api-app")
include("security")
include("seed-node-app")
include("settings")
include("support")
include("user")

includeBuild("network")
includeBuild("wallets")
