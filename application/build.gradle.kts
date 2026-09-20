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

// ResolverConfigTest parses ResolverConfig.java as text, so source-only edits such as wrapping a call across lines or
// changing a comment have to invalidate the task even though they leave the compiled class unchanged.
tasks.test {
    inputs.file("src/main/java/bisq/application/ResolverConfig.java")
        .withPropertyName("resolverConfigSource")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}
