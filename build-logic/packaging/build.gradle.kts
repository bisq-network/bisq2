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
        create("ProGuardPlugin") {
            id = "bisq.gradle.packaging.ProGuardPlugin"
            implementationClass = "bisq.gradle.packaging.ProGuardPlugin"
        }
    }
}

dependencies {
    implementation(project(":gradle-tasks"))
    implementation(project(":commons"))
    implementation(libs.commons.codec)
    implementation("com.guardsquare:proguard-gradle:7.5.0") {
        exclude(group = "com.guardsquare", module = "proguard-base")
        exclude(group = "com.guardsquare", module = "proguard-gradle")
    }
}

group = "bisq.gradle"
version = "0.1.0"
