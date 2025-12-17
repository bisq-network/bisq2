plugins {
    id("bisq.java-library")
    id("bisq.protobuf")
    id("bisq.grpc")
    application
}

repositories {
    maven {
        url = uri("https://jitpack.io")
    }
}

application {
    mainClass.set("bisq.oracle_node.OracleNodeApp")
}

dependencies {
    implementation("bisq:persistence")
    implementation("bisq:java-se")
    implementation("bisq:security")
    implementation("bisq:identity")
    implementation("bisq:bonded-roles")
    implementation("bisq:burningman")
    implementation("bisq:user")
    implementation("bisq:account")
    implementation("bisq:application")
    implementation("bisq:evolution")

    implementation("network:network")
    implementation("network:network-identity")

    implementation(libs.google.gson)
    implementation(libs.typesafe.config)
    implementation(libs.bundles.jackson)
}

tasks {
    distZip {
        enabled = false
    }

    distTar {
        enabled = false
    }
}


val grpcPluginId = "grpc"
val grpcArtifact = "io.grpc:protoc-gen-grpc-java:1.61.0" //1.71.0
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

