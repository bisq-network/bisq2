plugins {
    `kotlin-dsl` apply false
    java
}

tasks.named("clean") {
    dependsOn(subprojects.map { it.tasks.named("clean") })
}