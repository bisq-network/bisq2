plugins {
    id("bisq.java-library")
    id("bisq.gradle.bitcoin_core.BitcoinCorePlugin")
}

bitcoin_core {
    version.set("27.1")
}

sourceSets {
    main {
        resources {
            srcDir(layout.buildDirectory.file("generated/src/main/resources"))
        }
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":bitcoind"))
    implementation(project(":json-rpc"))

    implementation(libs.assertj.core)
    implementation(libs.junit.jupiter)
}