pluginManagement {
    repositories {
        mavenLocal()
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
include("contract")
include("daemon")
include("evolution")
include("identity")
include("i18n")
include("java-se")
include("offer")
include("os-specific")
include("persistence")
include("platform")
include("presentation")
include("http-api")
include("security")
include("settings")
include("support")
include("trade")
include("user")

includeBuild("apps")
includeBuild("network")
includeBuild("wallets")
includeBuild("wallets/bitcoind/build-logic")
