plugins {
    `java-library`
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://jitpack.io")
    }
}

java {
    toolchain {
        // We use the Java 22 toolchain to use jpackage to create the binaries.
        languageVersion.set(JavaLanguageVersion.of(22))
    }
}

tasks {
    compileJava {
        options.release.set(11)
    }

    test {
        useJUnitPlatform()
    }
}

val versionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
dependencies {
    versionCatalog.findLibrary("google-guava").ifPresent {
        implementation(it)
    }

    versionCatalog.findLibrary("lombok").ifPresent {
        compileOnly(it)
        annotationProcessor(it)
        testAnnotationProcessor(it)
        testCompileOnly(it)
    }

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

    versionCatalog.findLibrary("assertj-core").ifPresent {
        testImplementation(it)
    }

    versionCatalog.findLibrary("mockito").ifPresent {
        testImplementation(it)
    }
}
