plugins {
    id("bisq.java-library")
    alias(libs.plugins.openjfx)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

javafx {
    version = "21.0.11"
    modules = listOf("javafx.controls", "javafx.media")
}

sourceSets {
    main {
        resources {
            srcDir(layout.buildDirectory.file("generated/src/main/resources"))
        }
    }
}

dependencies {
    implementation("bisq:persistence")
    implementation("bisq:i18n")
    implementation("bisq:common")
    implementation("bisq:security")
    implementation("bisq:identity")
    implementation("bisq:account")
    implementation("bisq:offer")
    implementation("bisq:contract")
    implementation("bisq:trade")
    implementation("bisq:bonded-roles")
    implementation("bisq:settings")
    implementation("bisq:burningman")
    implementation("bisq:user")
    implementation("bisq:chat")
    implementation("bisq:support")
    implementation("bisq:presentation")
    implementation("bisq:notifications")
    implementation("bisq:bisq-easy")
    implementation("bisq:mu-sig")
    implementation("bisq:application")
    implementation("bisq:evolution")
    implementation("bisq:wallet")
    implementation("bisq:api")

    implementation("network:network")
    implementation("network:network-identity")
    implementation("network:i2p")

    implementation(libs.google.gson)
    implementation(libs.bundles.fontawesomefx)
    implementation(libs.bundles.fxmisc.libs)
    implementation(libs.typesafe.config)
    implementation(libs.zxing) {
        /* exclude(group = "org.bytedeco", module = "httpclient")*/
    }

    testImplementation(libs.testfx.junit5)
    testImplementation(libs.hamcrest)
    testRuntimeOnly(libs.testfx.monocle)
}

// Test classes annotated with @DesktopUiTest drive mouse and keyboard input with a TestFX robot. On a real
// desktop the robot controls the developer's pointer and keyboard focus, and on macOS it needs the
// Accessibility permission. They are excluded from the default test run and run only with uiTest.
val desktopUiTestTag = "ui"

tasks.named<Test>("test") {
    useJUnitPlatform {
        excludeTags(desktopUiTestTag)
    }
}

// Headless (Monocle) by default, so the robot only acts on a virtual screen.
// Use -PuiTest.native=true to run on the real desktop, for example to watch a test.
val uiTestNative = providers.gradleProperty("uiTest.native").map { it.toBoolean() }.orElse(false)

tasks.register<Test>("uiTest") {
    group = "verification"
    description = "Runs the JavaFX UI tests which drive input with a TestFX robot. Headless unless -PuiTest.native=true."
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    shouldRunAfter("test")
    useJUnitPlatform {
        includeTags(desktopUiTestTag)
    }

    systemProperty("testfx.robot", "glass")
    if (uiTestNative.get()) {
        systemProperty("testfx.headless", "false")
    } else {
        systemProperty("testfx.headless", "true")
        systemProperty("glass.platform", "Monocle")
        systemProperty("monocle.platform", "Headless")
        systemProperty("prism.order", "sw")
        systemProperty("prism.text", "t2k")
        systemProperty("java.awt.headless", "true")
    }
}
