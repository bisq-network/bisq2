plugins {
    id("bisq.java-library")
    application
}

application {
    mainClass.set("bisq.oracle_node_app.OracleNodeApp")
}

dependencies {
    implementation(project(":persistence"))
    implementation(project(":security"))
    implementation(project(":identity"))
    implementation(project(":bonded-roles"))
    implementation(project(":oracle-node"))
    implementation(project(":application"))

    implementation("network:network-common")
    implementation("network:network-identity")
    implementation("network:network")

    implementation(libs.google.gson)
    implementation(libs.typesafe.config)
}

tasks {
    distZip {
        enabled = false
    }

    distTar {
        enabled = false
    }
}
