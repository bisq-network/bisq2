plugins {
    java
}

val versionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
dependencies {
    testRuntimeOnly(versionCatalog.findLibrary("testfx-monocle").get())
}

// Configure the worker before any test can initialize JavaFX, including tests using Platform.startup.
tasks.withType<Test>().configureEach {
    systemProperty("testfx.robot", "glass")
    systemProperty("testfx.headless", "true")
    systemProperty("glass.platform", "Monocle")
    systemProperty("monocle.platform", "Headless")
    systemProperty("prism.order", "sw")
    systemProperty("java.awt.headless", "true")
}
