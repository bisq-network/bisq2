plugins {
    id("bisq.java-library")
}

dependencies {
    implementation(project(":network-common"))
    implementation(libs.bundles.i2p.v2)
}
