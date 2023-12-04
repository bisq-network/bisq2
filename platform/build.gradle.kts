plugins {
    `java-platform`
}

dependencies {
    constraints {
        api("org.checkerframework:checker-qual") {
            version { require("3.13.0") }
        }
        api("com.google.code.findbugs:jsr305") {
            version { require("3.0.2") }
        }
        api("com.google.errorprone:error_prone_annotations") {
            version { require("2.5.1") }
        }
        api("com.google.j2objc:j2objc-annotations") {
            version { require("1.3") }
        }
        api("com.google.guava:failureaccess") {
            version { require("1.0.1") }
        }
        api("com.google.guava:listenablefuture") {
            version { require("9999.0-empty-to-avoid-conflict-with-guava") }
        }
    }
}
