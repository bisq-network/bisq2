plugins {
    java
}

tasks.register("buildAll") {
    group = "build"
    description = "Build the entire project leaving it ready to work with."

    doLast {
        listOf(
            "build",
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

group = "bisq"

extensions.findByName("buildScan")?.withGroovyBuilder {
    setProperty("termsOfServiceUrl", "https://gradle.com/terms-of-service")
    setProperty("termsOfServiceAgree", "yes")
}