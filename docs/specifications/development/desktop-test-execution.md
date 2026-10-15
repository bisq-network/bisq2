# Desktop test execution

Ordinary test and build commands, including the build across all composite projects, must not
execute tests which use an input robot. Robot tests require an explicit, separate test task.
A test runner which bypasses Gradle's tag filters must skip these tests unless explicitly enabled.
Skipping must happen before a robot is constructed or its input backend is initialized.

Tests which need only the JavaFX application thread remain in the ordinary test suite. Starting
JavaFX in a Gradle test worker must use headless Monocle by default, including the desktop,
automation, and harness modules. This prevents ordinary builds from opening native windows or
initializing native GUI input backends.

Explicit robot test runs also use headless Monocle by default. A missing or incompatible headless
backend must fail the run, without falling back to the native desktop. Native robot input requires
a separate explicit opt-in which applies only to the robot test task.

Monocle confines GUI input to a virtual screen. It does not restrict test code's access to files,
the network, or process creation. Native robot mode may require macOS Accessibility permission
and can affect other applications through the real pointer and keyboard focus.

See [testing instructions](../../dev/testing.md#javafx-tests-and-input-robots) for commands and IDE setup.
