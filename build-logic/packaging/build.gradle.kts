plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

gradlePlugin {
    plugins {
        create("PackagingPlugin") {
            id = "bisq.gradle.packaging.PackagingPlugin"
            implementationClass = "bisq.gradle.packaging.PackagingPlugin"
        }
    }
}

dependencies {
    implementation("bitcoind-build-logic:gradle-tasks")
    implementation(libs.commons.codec)
}
