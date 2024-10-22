plugins {
    id("bisq.java-library")
}

dependencies {
    implementation(project(":common"))
    implementation(project(":security"))
    implementation(project(":persistence"))
    implementation(project(":settings"))
    implementation(project(":bonded-roles"))
    implementation(project(":application"))

    implementation("network:network")

    implementation(libs.typesafe.config)
    implementation(libs.bouncycastle.pg)
}
