plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":commons"))
    implementation(libs.bouncycastle.pg)
}