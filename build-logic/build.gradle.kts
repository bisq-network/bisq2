plugins {
    `kotlin-dsl` apply false
    java
}

tasks.named("clean") {
    dependsOn(subprojects.map { it.tasks.named("clean") })
}
tasks.named("build") {
    dependsOn(subprojects.map { it.tasks.named("build") })
}