## Building Bisq 2

1. **Clone Bisq 2**

   ```sh
   git clone https://github.com/bisq-network/bisq2
   cd bisq2
   ```

2. **Build Bisq**

   On macOS and Linux, execute:
   ```sh
   ./gradlew build
   ```

   On Windows:
   ```cmd
   gradlew.bat build
   ```

   If you prefer to skip tests to speed up the building process, just append `-x test` to the previous commands.

### Important notes

1. You do _not_ need to install Gradle to build Bisq. The `gradlew` shell script will install it for you, if necessary.

2. Bisq requires JDK 17. You can find out which
   version you have with:

   ```sh
   javac -version
   ```
3. Bisq requires [JavaFX](https://openjfx.io/openjfx-docs/) to be installed.

4. Adjustment for IntelliJ IDE (if used)
   Bisq developers use by default the IntelliJ development IDE (Community edition is free to use).
   If running from the IDE one need to enable `annotation processing` (search for that in the settings).
   The `Protocol Buffers` plugin need to be installed as well.

## Running Bisq

To run the Bisq 2 desktop app with Gradle and the default settings (using the Tor network) use:

```sh
./gradlew desktop:desktop-app:run
```

In that configuration the desktop app connects to the public seed nodes via the Tor network.

The default data directory is: `Bisq2`

For development, you might want to customize the options and use localhost instead of Tor. In that case you need to run
your own seed node as well. Bisq 2 use **JVM arguments** and has only limited support for **program arguments** (see
below).

### Running a development seed node with *JVM arguments*

First create the gradle installer script for the seed-node-app:
`./gradlew :apps:seed-node-app:installDist`

Pass the JVM arguments to the installer script:
```sh
JAVA_OPTS="-Dapplication.appName=bisq2_seed1 \
    -Dapplication.network.configByTransportType.clear.defaultNodePort=8000 \
    -Dapplication.network.supportedTransportTypes.0=CLEAR \
    -Dapplication.network.seedAddressByTransportType.clear.0=127.0.0.1:8000 \
    -Dapplication.network.seedAddressByTransportType.clear.1=127.0.0.1:8001" \
    apps/seed-node-app/build/install/seed-node-app/bin/seed-node-app
```

Optionally you can run a second seed node at port 8001:

```sh
JAVA_OPTS="-Dapplication.appName=bisq2_seed1 \
    -Dapplication.network.configByTransportType.clear.defaultNodePort=8001 \
    -Dapplication.network.supportedTransportTypes.0=CLEAR \
    -Dapplication.network.seedAddressByTransportType.clear.0=127.0.0.1:8000 \
    -Dapplication.network.seedAddressByTransportType.clear.1=127.0.0.1:8001" \
    apps/seed-node-app/build/install/seed-node-app/bin/seed-node-app
```

### Running a development desktop application with *JVM arguments*:

First create the gradle installer script for the desktop-app:
`./gradlew :desktop:desktop-app:installDist`

Pass the JVM arguments to the installer script:
```sh
JAVA_OPTS="-Dapplication.appName=bisq2_Alice_clear \
    -Dapplication.network.supportedTransportTypes.0=CLEAR \
    -Dapplication.network.seedAddressByTransportType.clear.0=127.0.0.1:8000 \
    -Dapplication.network.seedAddressByTransportType.clear.1=127.0.0.1:8001" \
    apps/desktop/desktop-app/build/install/desktop-app/bin/desktop-app
```

Optionally you can pass that data directory as *program argument* as follows:
`apps/desktop/desktop-app/build/install/desktop-app/bin/desktop-app --data-dir=<data_dir>`

You likely want to run a second desktop application for testing the trade use case with 2 traders (e.g. Alice and Bob).
Just change the `-Dapplication.appName` to something like `bisq2_Bob_clear` in the above configuration.
