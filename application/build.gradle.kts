plugins {
    id("bisq.java-library")
}

dependencies {
    implementation(project(":i18n"))
    implementation(project(":persistence"))
    implementation(project(":security"))
    implementation(project(":account"))
    implementation(project(":settings"))
    implementation(project(":user"))
    implementation(project(":burningman"))
    implementation(project(":chat"))
    implementation(project(":support"))
    implementation(project(":bonded-roles"))
    implementation(project(":offer"))
    implementation(project(":trade"))

    implementation("network:network:$version")

    implementation(libs.typesafe.config)
}
