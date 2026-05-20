plugins {
    `kotlin-dsl` apply false
    java
}

apply(from = "../gradle/dependency-security-overrides.gradle.kts")
apply(from = "../gradle/dependency-verification.gradle.kts")

tasks.named("clean") {
    dependsOn(subprojects.map { it.tasks.named("clean") })
}
tasks.named("build") {
    dependsOn(subprojects.map { it.tasks.named("build") })
}
