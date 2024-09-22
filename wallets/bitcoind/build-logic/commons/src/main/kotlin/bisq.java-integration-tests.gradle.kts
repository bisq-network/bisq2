plugins {
    java
}

sourceSets {
    create("integrationTest") {
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
}

val integrationTestImplementation by configurations.getting {
    extendsFrom(configurations.implementation.get())
}

val integrationTestRuntimeOnly by configurations.getting
configurations["integrationTestRuntimeOnly"].extendsFrom(configurations.runtimeOnly.get())

val versionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
dependencies {
    versionCatalog.findLibrary("lombok").ifPresent {
        "integrationTestAnnotationProcessor"(it)
        "integrationTestCompileOnly"(it)
    }

    versionCatalog.findLibrary("slf4j-api").ifPresent {
        "integrationTestImplementation"(it)
    }

    versionCatalog.findLibrary("logback-core").ifPresent {
        "integrationTestImplementation"(it)
    }
    versionCatalog.findLibrary("logback-classic").ifPresent {
        "integrationTestImplementation"(it)
    }

    versionCatalog.findLibrary("junit-jupiter").ifPresent {
        "integrationTestImplementation"(it)
    }

    versionCatalog.findLibrary("assertj-core").ifPresent {
        "integrationTestImplementation"(it)
    }
}

tasks.register<Test>("integrationTest") {
    description = "Runs integration tests."
    group = "verification"

    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    shouldRunAfter("test")

    useJUnitPlatform()

    testLogging {
        events("passed")
    }
}