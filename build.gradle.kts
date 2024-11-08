plugins {
    java
    id("bisq.gradle.maven_publisher.LocalMavenPublishPlugin")
}

tasks.register("buildAll") {
    group = "build"
    description = "Build the entire project leaving it ready to work with."

    doLast {
        listOf(
            "build",
            ":wallets:build",
            ":apps:seed-node-app:build",
            ":apps:seed-node-app:installDist",
            ":apps:desktop:desktop-app:build",
            ":apps:desktop:desktop-app:installDist",
            ":apps:desktop:desktop-app-launcher:generateInstallers",
//            ":REPLACEME:build",
        ).forEach {
            exec {
                println("Executing Build: $it")
                commandLine("./gradlew", it)
            }
        }
    }
}

tasks.register("cleanAll") {
    group = "build"
    description = "Cleans the entire project."

    doLast {
        listOf(
            ":apps:seed-node-app:clean",
            ":apps:oracle-node-app:clean",
            ":apps:rest-api-app:clean",
            ":apps:desktop:desktop-app:clean",
            ":apps:desktop:desktop:clean",
            ":apps:desktop:desktop-app-launcher:clean",
            ":apps:desktop:webcam-app:clean",
            ":bisq-easy:clean",
            ":persistence:clean",
            ":build-logic:clean", // not really necessary
            ":network:clean",
            ":application:clean",
            ":wallets:clean",
            "clean",
//            ":REPLACEME:clean",
        ).forEach {
            exec {
                println("Executing Clean: $it")
                commandLine("./gradlew", it)
            }
        }
    }
}

tasks.register("publishAll") {
    group = "publishing"
    description = "Publish all the jars in the following modules to local maven repository"

    doLast {
        listOf(
            ":account:publishToMavenLocal",
            ":application:publishToMavenLocal",
//            ":bisq-easy:publishToMavenLocal",
            ":bonded-roles:publishToMavenLocal",
            ":chat:publishToMavenLocal",
            ":common:publishToMavenLocal",
            ":contract:publishToMavenLocal",
            ":i18n:publishToMavenLocal",
            ":identity:publishToMavenLocal",
            ":network:publishToMavenLocal",
            ":network:tor:publishToMavenLocal",
            ":offer:publishToMavenLocal",
            ":persistence:publishToMavenLocal",
            ":platform:publishToMavenLocal",
            ":presentation:publishToMavenLocal",
            ":security:publishToMavenLocal",
            ":settings:publishToMavenLocal",
            ":support:publishToMavenLocal",
            ":trade:publishToMavenLocal",
            ":user:publishToMavenLocal",
//            ":wallets:publishToMavenLocal",
//            ":wallets:bitcoind:publishToMavenLocal",
        ).forEach {
            exec {
                println("Executing Publish To Maven Local: $it")
                commandLine("./gradlew", it)
            }
        }
    }
}

// for jitpack publishing
tasks.named("publishToMavenLocal").configure {
    dependsOn("publishAll")
}

extensions.findByName("buildScan")?.withGroovyBuilder {
    setProperty("termsOfServiceUrl", "https://gradle.com/terms-of-service")
    setProperty("termsOfServiceAgree", "yes")
}