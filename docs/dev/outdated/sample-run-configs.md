_Note: This document is outdated_

### IntelliJ IDEA: Application Run Configs

Here are a few IntelliJ IDEA run configurations for running two seeds and different desktop nodes.

#### Run Config: `Seed_1` (clearnet + tor + i2p)

Classpath of module (Alt+O)

```
apps.seed-node-app.main
```

Main Class (Alt+C)
```
bisq.seed_node.SeedNodeApp
```

Program Arguments (Alt+R)
```
--appName=bisq2_seed1
```

VM Options (Alt+V)
```
-Dapplication.network.configByTransportType.clear.defaultNodePort=8000 
-Dapplication.network.configByTransportType.tor.defaultNodePort=1000 
-Dapplication.network.configByTransportType.i2p.defaultNodePort=5000 
-Dapplication.network.supportedTransportTypes.0=TOR 
-Dapplication.network.supportedTransportTypes.1=I2P 
-Dapplication.network.supportedTransportTypes.2=CLEAR 
-Dapplication.network.seedAddressByTransportType.clear.0=127.0.0.1:8000 
-Dapplication.network.seedAddressByTransportType.clear.1=127.0.0.1:8001
```

#### Run Config: `Seed_2` (clearnet + tor + i2p)

Copy the `Seed_1` run configuration, rename it to `Seed_2` and change:
- Program Arguments to `--appName=bisq2_seed2`
- VM Options: adjust the ports in the following lines to
```
-Dapplication.network.configByTransportType.clear.defaultNodePort=8001 
-Dapplication.network.configByTransportType.tor.defaultNodePort=1001 
-Dapplication.network.configByTransportType.i2p.defaultNodePort=5001 
```

#### Run Config: `Alice_clear`

Classpath of module (Alt+O)
```
desktop.desktop-app.main
```

Main Class (Alt+C)
```
bisq.desktop_app.DesktopApp
```

Program Arguments (Alt+R)
```
--appName=bisq_Alice
```

VM Options (Alt+V)
```
-Dapplication.network.supportedTransportTypes.0=CLEAR 
-Dapplication.network.seedAddressByTransportType.clear.0=127.0.0.1:8000 
-Dapplication.network.seedAddressByTransportType.clear.1=127.0.0.1:8001
```

#### Run Config: `Alice_tor`

Copy the `Alice_clear` run configuration, rename it to `Alice_tor` and change:
- Program Arguments to `--appName=bisq_Alice_tor`
- VM options to:
```
-Dapplication.network.supportedTransportTypes.0=TOR 
-Dapplication.network.seedAddressByTransportType.tor.0=<onion address of seed 1>:1000 
-Dapplication.network.seedAddressByTransportType.tor.1=<onion address of seed 2>:1001
```

#### Run Config: `Alice_i2p`

Copy the `Alice_clear` run configuration, rename it to `Alice_i2p` and change:
- Program Arguments to `--appName=bisq_Alice_i2p`
- VM options to:
```
-Dapplication.network.supportedTransportTypes.0=I2P 
-Dapplication.network.seedAddressByTransportType.i2p.0=<i2p destination of seed 1>:5000 
-Dapplication.network.seedAddressByTransportType.i2p.1=<i2p destination of seed 2>:5001
```

### IntelliJ IDEA: Gradle run configs

* Create a new IntelliJ IDEA run config of type Gradle
* Choose a config name (e.g. `[gradle] Alice I2P`)
* Choose Gradle Project as `bisq2:desktop`
* Add VM options as necessary, in the format `-Dprop=value` (e.g. `-Dbisq.application.appName=bisq_Alice_i2p`)

### Command line: Gradle run configs

Start a seed with:

```
# Using default settings
./gradlew apps:seed-node-app:run
```

For example, to start two local seeds, `bisq2_seed1` and `bisq2_seed2`, reachable on clearnet:

```
# Seed 1
./gradlew apps:seed-node-app:run \
    -Dapplication.appName=bisq2_seed1 \
    -Dapplication.network.configByTransportType.clear.defaultNodePort=8000 \
    -Dapplication.network.supportedTransportTypes.0=CLEAR \
    -Dapplication.network.seedAddressByTransportType.clear.0=127.0.0.1:8000 \
    -Dapplication.network.seedAddressByTransportType.clear.1=127.0.0.1:8001

# Seed 2
./gradlew apps:seed-node-app:run \
    -Dapplication.appName=bisq2_seed2 \
    -Dapplication.network.configByTransportType.clear.defaultNodePort=8001 \
    -Dapplication.network.supportedTransportTypes.0=CLEAR \
    -Dapplication.network.seedAddressByTransportType.clear.0=127.0.0.1:8000 \
    -Dapplication.network.seedAddressByTransportType.clear.1=127.0.0.1:8001
```

Start a desktop client with:

```
# Using default settings
./gradlew apps:desktop:desktop-app:run
```

To start a custom desktop client connecting only to clearnet:

```
# Local client on clearnet only
./gradlew apps:desktop:desktop-app:run \
    -Dapplication.appName=bisq_Alice_clear \
    -Dapplication.network.supportedTransportTypes.0=CLEAR \
    -Dapplication.network.seedAddressByTransportType.clear.0=127.0.0.1:8000 \
    -Dapplication.network.seedAddressByTransportType.clear.1=127.0.0.1:8001
```