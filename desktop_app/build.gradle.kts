plugins {
    id("bisq.java-library")
    id("bisq.gradle.desktop.regtest.BisqDesktopRegtestPlugin")
    application
    alias(libs.plugins.openjfx)
    alias(libs.plugins.shadow)
}

application {
    mainClass.set("bisq.desktop_app.DesktopApp")
}

javafx {
    version = "17.0.1"
    modules = listOf("javafx.controls")
}

dependencies {
    implementation(project(":persistence"))
    implementation(project(":i18n"))
    implementation(project(":security"))
    implementation(project(":identity"))
    implementation(project(":account"))
    implementation(project(":offer"))
    implementation(project(":contract"))
    implementation(project(":trade"))
    implementation(project(":bonded_roles"))
    implementation(project(":settings"))
    implementation(project(":user"))
    implementation(project(":chat"))
    implementation(project(":support"))
    implementation(project(":presentation"))
    implementation(project(":bisq_easy"))
    implementation(project(":application"))
    implementation(project(":desktop"))

    implementation("network:network")
    implementation("wallets:electrum")
    implementation("wallets:bitcoind")

    implementation(libs.google.guava)
    implementation(libs.typesafe.config)
}

tasks {
    named<Jar>("jar") {
        manifest {
            attributes(
                mapOf(
                    Pair("Implementation-Title", project.name),
                    Pair("Implementation-Version", project.version),
                    Pair("Main-Class", "bisq.desktop_app.DesktopApp")
                )
            )
        }
    }

    distZip {
        enabled = false
    }

    distTar {
        enabled = false
    }

    shadowDistZip {
        enabled = false
    }

    shadowDistTar {
        enabled = false
    }

    shadowJar {
        enabled = false
    }
}
