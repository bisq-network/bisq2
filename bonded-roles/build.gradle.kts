plugins {
    id("bisq.java-library")
    id("bisq.protobuf")
}

dependencies {
    annotationProcessor("network:storage-policy-processor:$version")
    testAnnotationProcessor("network:storage-policy-processor:$version")
    implementation(project(":persistence"))
    implementation(project(":security"))
    implementation(project(":i18n"))
    implementation(project(":identity"))
    implementation(project(":settings"))

    implementation("network:network-identity:$version")
    implementation("network:network:$version")

    implementation(libs.google.gson)
    implementation(libs.typesafe.config)
    implementation(libs.bundles.jackson)
}
