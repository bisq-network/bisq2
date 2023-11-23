plugins {
    id("bisq.java-library")
    id("bisq.protobuf")
}

dependencies {
    api(platform(libs.square.okhttp.bom))
    implementation(platform(libs.square.okhttp.bom))

    api("com.squareup.okhttp3:okhttp")
    api(libs.square.moshi)

    implementation("com.squareup.okhttp3:logging-interceptor")

    testImplementation(libs.assertj.core)
    testImplementation("com.squareup.okhttp3:mockwebserver")
}
