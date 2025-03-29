plugins {
    id("bisq.java-library")
    id("bisq.protobuf")
    id("bisq.gradle.electrum.BisqElectrumPlugin")
    id("bisq.java-integration-tests")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

electrum {
    version.set("4.2.2")
}

sourceSets {
    main {
        resources {
            srcDir(layout.buildDirectory.file("generated/src/main/resources"))
        }
    }
}

dependencies {
    api("bitcoind:core")
    implementation("bitcoind:json-rpc")
    implementation("bitcoind:regtest")
    implementation("bisq:java-se")
    implementation("wallets:wallet")

    implementation(project(":process"))
    implementation(libs.typesafe.config)
    implementation(libs.bundles.glassfish.jersey)

    integrationTestImplementation("bitcoind:bitcoind")
    integrationTestImplementation("bitcoind:regtest")
}