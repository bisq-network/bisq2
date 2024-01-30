plugins {
    id("bisq.protobuf")
}

val versionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
dependencies {
    versionCatalog.findLibrary("grpc-netty-shaded").ifPresent {
        runtimeOnly(it)
    }

    versionCatalog.findBundle("grpc").ifPresent {
        implementation(it)
    }

    versionCatalog.findLibrary("apache-tomcat-annotations-api").ifPresent {
        compileOnly(it)
    }
}