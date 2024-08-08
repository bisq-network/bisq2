plugins {
    id("bisq.gradle.packaging.ProGuardPlugin") version "0.1.0"
}

proguardConfig {
    rulesFileRelativePath = "../../"
}

extensions.findByName("buildScan")?.withGroovyBuilder {
    setProperty("termsOfServiceUrl", "https://gradle.com/terms-of-service")
    setProperty("termsOfServiceAgree", "yes")
}