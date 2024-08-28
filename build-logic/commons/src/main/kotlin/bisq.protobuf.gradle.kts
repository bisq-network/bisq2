plugins {
    java
    idea
    id("com.google.protobuf")
}

repositories {
    mavenCentral()
}

val versionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
dependencies {
    versionCatalog.findLibrary("protobuf-java").ifPresent {
        implementation(it)
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.19.4"
    }
}