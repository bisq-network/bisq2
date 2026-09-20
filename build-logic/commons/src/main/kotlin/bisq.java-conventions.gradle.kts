plugins {
    `java-library`
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://jitpack.io")
    }
}

val pinnedJavaLanguageVersion = providers.gradleProperty("releaseBuild.javaVersion")
    .map { it.substringBefore('.').toInt() }
    .orElse(21)

java {
    toolchain {
        languageVersion.set(pinnedJavaLanguageVersion.map { JavaLanguageVersion.of(it) })
        vendor.set(JvmVendorSpec.AZUL)
    }
}

tasks {
    test {
        useJUnitPlatform()
    }

    withType<Jar> {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
        // Pinned so the entry modes don't follow the umask of the machine that compiled the classes
        // and checked out the resources.
        dirPermissions { unix("0755") }
        filePermissions { unix("0644") }
    }
}

val versionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
dependencies {
    versionCatalog.findLibrary("google-guava").ifPresent {
        implementation(it)
    }

    versionCatalog.findLibrary("findbugs-jsr305").ifPresent {
        compileOnly(it)
        testCompileOnly(it)
    }

    versionCatalog.findLibrary("lombok").ifPresent {
        compileOnly(it)
        annotationProcessor(it)
        testAnnotationProcessor(it)
        testCompileOnly(it)
    }

    // Verifies at compile time that a network payload type declares its storage properties. Applied to every module
    // rather than to a maintained list, so a payload type cannot be added to a module that forgot to wire it. The
    // processor declares itself ISOLATING, so incremental compilation stays on. The processor module itself does
    // not apply this convention, so it cannot end up on its own processor path.
    annotationProcessor("network:storage-policy-processor")
    testAnnotationProcessor("network:storage-policy-processor")

    versionCatalog.findLibrary("slf4j-api").ifPresent {
        implementation(it)
    }

    versionCatalog.findLibrary("logback-core").ifPresent {
        implementation(it)
    }
    versionCatalog.findLibrary("logback-classic").ifPresent {
        implementation(it)
    }

    versionCatalog.findLibrary("junit-jupiter").ifPresent {
        testImplementation(it)
    }
    versionCatalog.findLibrary("junit-platform-launcher").ifPresent {
        testRuntimeOnly(it)
    }

    versionCatalog.findLibrary("assertj-core").ifPresent {
        testImplementation(it)
    }

    versionCatalog.findLibrary("mockito").ifPresent {
        testImplementation(it)
    }
}
