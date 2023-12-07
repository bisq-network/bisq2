plugins {
    id("bisq.java-library")
    application
    alias(libs.plugins.shadow)
}

application {
    mainClass.set("bisq.seed_node.SeedNodeApp")
}

dependencies {
    implementation(project(":persistence"))
    implementation(project(":security"))
    implementation(project(":bonded_roles"))
    implementation(project(":application"))
    implementation(project(":identity"))

    implementation("network:network-common")
    implementation("network:network")
    implementation("network:network-identity")

    implementation(libs.typesafe.config)
    implementation(libs.google.gson)
}

tasks {
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
