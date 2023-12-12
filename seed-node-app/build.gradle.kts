plugins {
    id("bisq.java-library")
    application
}

application {
    mainClass.set("bisq.seed_node.SeedNodeApp")
}

dependencies {
    implementation(project(":persistence"))
    implementation(project(":security"))
    implementation(project(":bonded-roles"))
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
}
