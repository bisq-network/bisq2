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
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks {
    test {
        useJUnitPlatform()
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
