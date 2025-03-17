plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

gradlePlugin {
    plugins {
        create("LocalMavenPublishPlugin") {
            id = "bisq.gradle.maven_publisher.LocalMavenPublishPlugin"
            implementationClass = "bisq.gradle.maven_publisher.LocalMavenPublishPlugin"
        }
    }
}
