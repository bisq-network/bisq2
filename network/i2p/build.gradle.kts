plugins {
    id("bisq.java-library")
}

dependencies {
    // Exclude httpclient transitive dependency because it may override
    // I2P-specific impl of some Apache classes like org.apache.http.util.Args
    implementation(libs.apache.httpcomponents.core) {
        exclude(group = "org.apache.httpcomponents", module = "httpclient")
    }
    implementation("net.java.dev.jna:jna:5.13.0")
    implementation(libs.bundles.i2p)
    implementation("bisq:security")
}
