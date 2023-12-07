plugins {
    id("bisq.java-library")
    id("bisq.java-integration-tests")
    id("bisq.gradle.tor_binary.BisqTorBinaryPlugin")
}

tor {
    version.set("12.0.5")
}

sourceSets {
    main {
        resources {
            srcDir(layout.buildDirectory.file("generated/src/main/resources"))
        }
    }
}

dependencies {
    implementation(project(":tor-common"))

    implementation("network:network-common")
    implementation("network:network-identity")
    implementation("network:socks5-socket-channel")

    implementation(libs.google.guava)
    implementation(libs.failsafe)
    implementation(libs.tukaani)
    implementation(libs.typesafe.config)

    implementation(libs.chimp.jsocks)
    implementation(libs.chimp.jtorctl)
}