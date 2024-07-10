plugins {
    id("bisq.java-library")
    id("bisq.gradle.copy_version.CopyWebcamAppVersionPlugin")
    alias(libs.plugins.openjfx)
}

javafx {
    version = "17.0.10"
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

    implementation("network:network-common")
    implementation("network:network")
    implementation("network:network-identity")

    implementation("wallets:core")
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


tasks {
    named<DefaultTask>("build") {
        dependsOn(project.tasks.named("copyWebcamAppVersion"))
    }
}