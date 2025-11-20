import java.util.Properties

// Function to read properties from a file
fun readPropertiesFile(filePath: String): Properties {
    val properties = Properties()
    file(filePath).inputStream().use { properties.load(it) }
    return properties
}

plugins {
    id("bisq.java-library")
    application
}

// Read version from root gradle.properties
val properties = readPropertiesFile("../../gradle.properties")
val rootVersion = properties.getProperty("version", "unspecified")
version = rootVersion

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(22))
    }
}

application {
    mainClass.set("bisq.http_api_node.HttpApiApp")
}

dependencies {
    implementation("bisq:persistence")
    implementation("bisq:java-se")
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
    implementation("bisq:evolution")
    implementation("bisq:os-specific")
    implementation("bisq:http-api")

    implementation("network:network")
    implementation("bitcoind:core")
    implementation("wallets:wallet")

    implementation(libs.typesafe.config)
    implementation(libs.bundles.rest.api.libs)
}

distributions {
    main {
        distributionBaseName.set("bisq-trusted-node")
        contents {
            // Include README and documentation
            from("${projectDir}/distribution") {
                include("README.md", "LICENSE", "CHANGELOG.md", "trusted-node.properties", "run-trusted-node.sh", "run-trusted-node.bat")
            }
            // Make shell script executable (493 decimal = 0755 octal)
            filesMatching("run-trusted-node.sh") {
                mode = 493
            }
        }
    }
}

tasks {
    distZip {
        enabled = true
    }

    distTar {
        enabled = true
        compression = Compression.GZIP
        archiveExtension.set("tar.gz")
    }
}
