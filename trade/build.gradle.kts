plugins {
    id("bisq.java-library")
    id("bisq.protobuf")
    id("bisq.grpc")
}

dependencies {
    implementation(project(":persistence"))
    implementation(project(":i18n"))
    implementation(project(":security"))
    implementation(project(":identity"))
    implementation(project(":user"))
    implementation(project(":account"))
    implementation(project(":offer"))
    implementation(project(":contract"))
    implementation(project(":support"))
    implementation(project(":chat"))
    implementation(project(":settings"))
    implementation(project(":presentation"))
    implementation(project(":bonded-roles"))

    implementation("network:network:$version")
    implementation("network:network-identity:$version")

    implementation(libs.typesafe.config)
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
