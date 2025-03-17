plugins {
    id("bisq.java-library")
    alias(libs.plugins.openjfx)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(22))
    }
    sourceCompatibility = JavaVersion.VERSION_22
    targetCompatibility = JavaVersion.VERSION_22
}

javafx {
    version = "22.0.1"
    modules = listOf("javafx.controls", "javafx.media")
}

sourceSets {
    main {
        resources {
            srcDir(layout.buildDirectory.file("generated/src/main/resources"))
        }
    }
}

dependencies {
    implementation("bisq:persistence")
    implementation("bisq:i18n")
    implementation("bisq:common")
    implementation("bisq:security")
    implementation("bisq:identity")
    implementation("bisq:account")
    implementation("bisq:offer")
    implementation("bisq:contract")
    implementation("bisq:trade")
    implementation("bisq:bonded-roles")
    implementation("bisq:settings")
    implementation("bisq:user")
    implementation("bisq:chat")
    implementation("bisq:support")
    implementation("bisq:presentation")
    implementation("bisq:bisq-easy")
    implementation("bisq:application")
    implementation("bisq:evolution")

    implementation("network:network")
    implementation("network:network-identity")

    implementation("bitcoind:core")
    implementation("wallets:wallet")
    // implementation("wallets:electrum")
    // implementation("wallets:bitcoind")

    implementation(libs.google.gson)
    implementation(libs.bundles.fontawesomefx)
    implementation(libs.bundles.fxmisc.libs)
    implementation(libs.typesafe.config)
    implementation(libs.zxing) {
        /* exclude(group = "org.bytedeco", module = "httpclient")*/
    }

    testImplementation(libs.testfx.junit5)
    testImplementation(libs.openjfx.monocle)
}

