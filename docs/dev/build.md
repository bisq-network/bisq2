## Building Bisq 2

1. **Clone Bisq 2**
   ```bash
   git clone https://github.com/bisq-network/bisq2.git
   ```
   ```bash
   cd bisq2
   ```

2. **Install Dependencies:**
   Bisq requires JDK 21. On Linux, you will also need a working tor executable, which is distributed with the tor daemon and the tor browser packages.

   Please see the official [Tor installation instructions](https://support.torproject.org/little-t-tor/getting-started/installing/).
   The TLDR for Debian-based distros:
   ```bash
    sudo apt update
    sudo apt install apt-transport-https curl

    # Add the Tor Project GPG key
    curl https://deb.torproject.org/torproject.org/gpgkey | sudo gpg --dearmor -o /usr/share/keyrings/tor-archive-keyring.gpg

    # Add Tor repository
    echo "deb [signed-by=/usr/share/keyrings/tor-archive-keyring.gpg] \
    https://deb.torproject.org/torproject.org $(lsb_release -cs) main" \
    | sudo tee /etc/apt/sources.list.d/tor.list

    # Install Tor
    sudo apt update
    sudo apt install tor

   ```

3. **Update to latest GitHub version:**
   ```bash
   git pull
   ```

4. **Build Bisq**

   On macOS and Linux, execute:
   ```sh
   ./gradlew clean build
   ```

   On Windows:
   ```cmd
   gradlew.bat clean build
   ```

   If you prefer to skip tests to speed up the building process, just append `-x test` to the previous commands.


5. **Generate Seed Node & Desktop App Binaries**

   Seed Node:
   ```sh
   ./gradlew :apps:seed-node-app:clean :apps:seed-node-app:installDist
   ```
   Desktop:
   ```sh
   ./gradlew :apps:desktop:desktop-app:clean :apps:desktop:desktop-app:installDist
   ```

   **For Windows environments**: replace ./gradlew with gradle.bat as the previous example shows


6. **Generate Installers**

   ```sh
   ./gradlew :apps:desktop:desktop-app-launcher:clean :apps:desktop:desktop-app-launcher:generateInstallers
   ```

7. **Other useful dev gradle commands**

For a quick full cleanup/rebuild you can use

   ```sh
   ./gradlew cleanAll buildAll
   ```


### Important notes

1. You do _not_ need to install Gradle to build Bisq. The `gradlew` shell script will install it for you, if necessary.

2. Bisq requires JDK 21. You can find out which
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
apps/desktop/desktop-app/build/install/desktop-app/bin/desktop-app
```

In that configuration the desktop app connects to the public seed nodes via the Tor network.

The default data directory is: `Bisq2`

For development, you might want to customize the options and use localhost instead of Tor. In that case you need to run
your own seed node as well. Bisq 2 use **JVM arguments** and has only limited support for **program arguments** (see
below).

## How to pass options

### JVM arguments

We use the [typesafe config](https://github.com/lightbend/config) framework which expects JVM arguments.

One can pass JVM options in the [IntelliJ IDE Run Configuration](https://i.sstatic.net/gMWQX.png) or add it
as `JAVA_OPTS` to gradle sh installer scripts:

```sh
JAVA_OPTS="-Dapplication.appName=bisq2_seed1 \
    -Dapplication.network.configByTransportType.clear.defaultNodePort=8000 \
    -Dapplication.network.supportedTransportTypes.0=CLEAR" \
    apps/seed-node-app/build/install/seed-node-app/bin/seed-node-app
```

Adding JVM options to the binary can be done as follows:

```
[PATH TO BINARY] -Dapplication.appName=bisq2_Alice_clear \
	    -Dapplication.network.supportedTransportTypes.0=CLEAR
```

_Note, that the `Bisq 2` binary has a space in the file name, so you need to use a `backslash` before the
space (`Bisq\ 2`)._

### Supported program arguments

Additionally, to the JVM options we support 2 program arguments:

`--app-name` and `--data-dir`. Option name and value is seperated with `=`.

`--app-name` has the same function as `appName` in the config.

Program arguments can be added directly to the gradle sh installer scripts:

`apps/desktop/desktop-app/build/install/desktop-app/bin/desktop-app --data-dir=<data_dir>`

### Adding custom config file

A custom config file with the file name `bisq.conf` can be added to the data directory.
If a custom data directory is used it should be provided by a program argument or as JVM argument,
otherwise the config file would be expected in the default `Bisq2` data directory.

The custom config file overrides the default config files for the entries which are defined in the custom file.
The default config file can be found at:
https://github.com/bisq-network/bisq2/blob/main/apps/desktop/desktop-app/src/main/resources/desktop.conf

As this file gets frequently updated with new releases, one should only use the entries which one wants to override.
The structure from the default config must be maintained.

#### Example:

Overriding the marketPrice provider with a self-hosted one would be done as follows:

```
application {
    bondedRoles = {
        marketPrice = {
            enabled = true
            providers = [
                        {
                            url = "http://[MY_ONION_ADDRESS].onion"
                            operator = "my own node",
                        }
                    ]
        }
    }
}
```

Only change entries which are clear to you as inappropriate values could lead to issues.

## Run developer setup

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
JAVA_OPTS="-Dapplication.appName=bisq2_seed2 \
    -Dapplication.network.configByTransportType.clear.defaultNodePort=8001 \
    -Dapplication.network.supportedTransportTypes.0=CLEAR \
    -Dapplication.network.seedAddressByTransportType.clear.0=127.0.0.1:8000 \
    -Dapplication.network.seedAddressByTransportType.clear.1=127.0.0.1:8001" \
    apps/seed-node-app/build/install/seed-node-app/bin/seed-node-app
```

### Running a development desktop application with *JVM arguments*:

First create the gradle installer script for the desktop-app:
`./gradlew :apps:desktop:desktop-app:installDist`

Pass the JVM arguments to the installer script:
```sh
JAVA_OPTS="-Dapplication.appName=bisq2_Alice_clear \
    -Dapplication.network.supportedTransportTypes.0=CLEAR \
    -Dapplication.network.seedAddressByTransportType.clear.0=127.0.0.1:8000 \
    -Dapplication.network.seedAddressByTransportType.clear.1=127.0.0.1:8001" \
    apps/desktop/desktop-app/build/install/desktop-app/bin/desktop-app
```

Optionally you can pass that data directory as *program argument* as follows:
`apps/desktop/desktop-app/build/install/desktop-app/bin/desktop-app --data-dir=bisq2_Alice_tor`

Note, that in that case it runs with the default config (using Tor).

You likely want to run a second desktop application for testing the trade use case with 2 traders (e.g. Alice and Bob).
Just change the `-Dapplication.appName` to something like `bisq2_Bob_clear` in the above configuration.

### Setting devMode and devModeReputationScore
A seller requires reputation for trading. This can be achieved in dev environment with setting up an oracle node and Bisq 1, though this setup is a bit advanced.
To make it easier for devs, we added a `devModeReputationScore` field to the config. It also requires that the `devMode` flag is set to true.
Set the desired reputation score to `devModeReputationScore` and apply that to all your trade apps. This value will not 
be applied per user profile but overrides the reputation score lookup globally.
For proper release testing a correct setup should be used, as this workaround does not cover all the use cases for reputation.
```sh
JAVA_OPTS="-Dapplication.appName=bisq2_Alice_clear \
    -Dapplication.network.supportedTransportTypes.0=CLEAR \
    -Dapplication.network.seedAddressByTransportType.clear.0=127.0.0.1:8000 \
    -Dapplication.network.seedAddressByTransportType.clear.1=127.0.0.1:8001
    -Dapplication.devMode=true \
    -Dapplication.devModeReputationScore=50000" \
    apps/desktop/desktop-app/build/install/desktop-app/bin/desktop-app
```


