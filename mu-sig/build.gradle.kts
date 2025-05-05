plugins {
    id("bisq.java-library")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    implementation(project(":persistence"))
    implementation(project(":i18n"))
    implementation(project(":security"))
    implementation(project(":identity"))
    implementation(project(":account"))
    implementation(project(":offer"))
    implementation(project(":contract"))
    implementation(project(":trade"))
    implementation(project(":bonded-roles"))
    implementation(project(":settings"))
    implementation(project(":user"))
    implementation(project(":chat"))
    implementation(project(":support"))
    implementation(project(":presentation"))

    implementation("network:network:$version")
    implementation("network:network-identity:$version")

    implementation(libs.google.gson)
    implementation(libs.typesafe.config)

    testImplementation(libs.testfx.junit5)
}
