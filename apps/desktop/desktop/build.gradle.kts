plugins {
    id("bisq.java-library")
    alias(libs.plugins.openjfx)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

javafx {
    version = "21.0.6"
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
    implementation("bisq:burningman")
    implementation("bisq:user")
    implementation("bisq:chat")
    implementation("bisq:support")
    implementation("bisq:presentation")
    implementation("bisq:bisq-easy")
    implementation("bisq:mu-sig")
    implementation("bisq:application")
    implementation("bisq:evolution")
    implementation("bisq:wallet")
    implementation("bisq:http-api")

    implementation("network:network")
    implementation("network:network-identity")
    implementation("network:i2p")

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

