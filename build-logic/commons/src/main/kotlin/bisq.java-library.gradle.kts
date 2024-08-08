plugins {
    id("bisq.java-conventions")
}

dependencies {
    api(platform("bisq:platform"))
    implementation("com.guardsquare:proguard-gradle:7.5.0") {
        exclude(group = "com.guardsquare", module = "proguard-base")
        exclude(group = "com.guardsquare", module = "proguard-gradle")
    }
    implementation("bisq:common")
    testImplementation("bisq:common")
}