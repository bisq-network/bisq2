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
    implementation("com.google.protobuf:protobuf-java:4.27.3")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.27.3"
    }
//    generateProtoTasks {
//        all().forEach { task ->
//            task.builtins {
//                create("java") {
//                    option("lite")
//                }
//            }
//        }
//    }
}

tasks.withType<JavaCompile> {
    dependsOn(tasks.named("generateProto"))
}

sourceSets {
    main {
        java {
            srcDir("build/generated/source/proto/main/java")
        }
    }
    test {
        java {
            srcDir("build/generated/source/proto/test/java")
        }
    }
}