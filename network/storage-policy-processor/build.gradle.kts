plugins {
    `java-library`
}

repositories {
    mavenCentral()
}

// Deliberately does not apply bisq.java-conventions. This module lands on the annotation processor classpath of every
// module that defines a payload type, and the convention adds guava, slf4j and logback as implementation
// dependencies, which would come with it. The processor uses none of them.
//
// It also has no dependency on any bisq module, because it is applied to the module that defines the storage types,
// so depending on it would be circular. It matches them by fully qualified name through javax.lang.model instead.

val pinnedJavaLanguageVersion = providers.gradleProperty("releaseBuild.javaVersion")
    .map { it.substringBefore('.').toInt() }
    .orElse(21)

java {
    toolchain {
        languageVersion.set(pinnedJavaLanguageVersion.map { JavaLanguageVersion.of(it) })
        vendor.set(JvmVendorSpec.AZUL)
    }
}

dependencies {
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
