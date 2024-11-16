plugins {
    id("bisq.java-library")
    id("bisq.protobuf")
}

dependencies {
    implementation(project(":i18n"))
    implementation(project(":persistence"))
    implementation(project(":security"))
    implementation(project(":identity"))
    implementation(project(":bonded-roles"))

    implementation("network:network:$version")
    implementation("network:network-identity:$version")

    implementation(libs.google.gson)
    implementation(libs.typesafe.config)
    implementation(libs.swagger.jaxrs2.jakarta)
    implementation(libs.bundles.glassfish.jersey)
    implementation(libs.bundles.jackson)
}
