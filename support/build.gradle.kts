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
    implementation(project(":user"))
    implementation(project(":chat"))
    implementation(project(":account"))
    implementation(project(":offer"))
    implementation(project(":contract"))
    implementation(project(":bonded-roles"))

    implementation("network:network:$version")
    implementation("network:network-identity:$version")

    implementation(libs.google.gson)
    implementation(libs.typesafe.config)
}
