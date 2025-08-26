import bisq.gradle.common.VersionUtil
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("bisq.java-library")
    id("bisq.gradle.i2p_router.I2pRouterAppPlugin")
    application
    alias(libs.plugins.shadow)
    alias(libs.plugins.openjfx)
    id("bisq.protobuf")
    id("bisq.grpc")
}

application {
    mainClass.set("bisq.i2p_router.I2PRouterAppLauncher")
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
    implementation("bisq:security")
    implementation("network:network")
    implementation("network:i2p")

    implementation(libs.fxmisc.easybind)
    implementation(libs.typesafe.config)
    implementation(libs.bundles.i2p)
}


tasks {
    named<Jar>("jar") {
        manifest {
            attributes(
                mapOf(
                    Pair("Implementation-Title", project.name),
                    Pair("Implementation-Version", project.version),
                    Pair("Main-Class", "bisq.i2p_router.I2PRouterAppLauncher")
                )
            )
        }
    }

    named<ShadowJar>("shadowJar") {
        val version = VersionUtil.getVersionFromFile(project)
        archiveClassifier.set("$version-all")
    }

    distZip {
        enabled = false
    }

    distTar {
        enabled = false
    }
}
