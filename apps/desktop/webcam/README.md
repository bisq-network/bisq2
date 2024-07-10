# Webcam app

The webcam application runs as independent jvm process to reduce security risks from the many dependencies it uses (many
native drivers).
We start from the desktop application that app and communicate via TPC sockets the resulting qr code and simple control
commands like `shutdown`.
As there is no build dependency from desktop to the webcam app to avoid the dependency inclusion we need to build a jar
file zip it and copy it into desktop so that it can be used by the Bisq app.
We use the java executable which is included in the binary. We unzip the zip file from the resources into the
application directory and use that path for the Processbuilder.

## How to build to make it accessible in desktop?

To include the resources in the desktop application it requires to run the gradle task `processWebcamForDesktop` in the
webcam project (subproject of desktop).
This task creates a shadow jar, makes a zip and copies the zip to the build directory in the `desktop:desktop` project.
The target directoy is `apps/desktop/desktop/build/generated/src/main/resources/webcam-app`.
It also copies teh `version.txt` file from the `desktop:webcam` projects root directory to the same resource directory.

`