import java.util.Locale.getDefault

plugins {
    java
    id("bisq.gradle.maven_publisher.LocalMavenPublishPlugin")
}

fun getGradleCommand(): String {
    return if (System.getProperty("os.name").lowercase(getDefault()).contains("win")) "gradlew.bat" else "./gradlew"
}

tasks.register("buildAll") {
    group = "build"
    description = "Build the entire project leaving it ready to work with."

    doLast {
        listOf(
            ":build-logic:build",
            "build",
            ":network:build",
            ":network:tor:build",
            ":apps:seed-node-app:build",
            ":apps:oracle-node-app:build",
            ":apps:http-api-app:build",
            ":apps:node-monitor-web-app:build",
            ":apps:desktop:desktop:build",
            ":apps:desktop:desktop-app:build",
            ":apps:desktop:desktop-app-launcher:build",
            ":apps:desktop:webcam-app:build",
            ":apps:desktop:bi2p:build",
        ).forEach {
            exec {
                println("Executing Build: $it")
                commandLine(getGradleCommand(), it)
            }
        }
    }
}

tasks.register("cleanAll") {
    group = "build"
    description = "Cleans the entire project."

    doLast {
        listOf(
            ":build-logic:clean",
            "clean",
            ":network:clean",
            ":network:tor:clean",
            ":apps:seed-node-app:clean",
            ":apps:oracle-node-app:clean",
            ":apps:http-api-app:clean",
            ":apps:node-monitor-web-app:clean",
            ":apps:desktop:desktop:clean",
            ":apps:desktop:desktop-app:clean",
            ":apps:desktop:desktop-app-launcher:clean",
            ":apps:desktop:webcam-app:clean",
            ":apps:desktop:bi2p:clean",
        ).forEach {
            exec {
                println("Executing Clean: $it")
                commandLine(getGradleCommand(), it)
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
            ":burningman:publishToMavenLocal",
            ":application:publishToMavenLocal",
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
            ":bisq-easy:publishToMavenLocal",
        ).forEach {
            exec {
                println("Executing Publish To Maven Local: $it")
                commandLine(getGradleCommand(), it)
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