plugins {
    id("bisq.java-library")
    id("bisq.protobuf")
    id("bisq.grpc")
}

// The programs of the source set demo try the I2P library by hand and need a running I2P router. They are not tests,
// and Gradle fails a test task whose test source set contains no tests.
val demo: SourceSet by sourceSets.creating

configurations[demo.implementationConfigurationName].extendsFrom(configurations.implementation.get())

dependencies {
    implementation("bisq:security")
    implementation(libs.bundles.i2p)
    implementation(libs.grpc.netty.shaded)

    "demoCompileOnly"(libs.lombok)
    "demoAnnotationProcessor"(libs.lombok)
}

// The build compiles the demo programs, so that it finds compile errors in them
tasks.named("assemble") {
    dependsOn(demo.classesTaskName)
}

val grpcPluginId = "grpc"
val grpcArtifact = "io.grpc:protoc-gen-grpc-java:1.81.0"
val protocArtifact = "com.google.protobuf:protoc:4.28.2"

protobuf {
    protoc {
        artifact = protocArtifact
    }

    plugins {
        // This must be declared before `generateProtoTasks`
        create(grpcPluginId) {
            artifact = grpcArtifact
        }
    }

    // We refer to the name as a string after it's declared
    generateProtoTasks {
        ofSourceSet("main").configureEach {
            plugins {
                create(grpcPluginId)
            }
        }
    }
}

sourceSets["main"].java.srcDirs(
    "build/generated/source/proto/main/grpc",
    "build/generated/source/proto/main/java"
)

tasks.named<JavaCompile>("compileJava") {
    dependsOn("generateProto")
}

idea {
    module {
        generatedSourceDirs.add(file("build/generated/source/proto/main/java"))
        generatedSourceDirs.add(file("build/generated/source/proto/main/grpc"))
    }
}


