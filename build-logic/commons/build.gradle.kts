plugins {
    `groovy-gradle-plugin`
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(libs.protobuf.gradle.plugin)
}