plugins {
    id("bisq.java-library")
    id("bisq.protobuf")
}

dependencies {
    annotationProcessor("network:storage-policy-processor:$version")
    testAnnotationProcessor("network:storage-policy-processor:$version")
    implementation(project(":i18n"))
    implementation(project(":persistence"))
    implementation(project(":bonded-roles"))

    implementation("network:network:$version")
    implementation("network:network-identity:$version")
}
