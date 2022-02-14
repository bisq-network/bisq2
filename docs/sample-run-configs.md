### IntelliJ IDEA: Application Run Configs

Here are a few IntelliJ IDEA run configurations for running two seeds and different desktop nodes.

#### Run Config: `Seed_1` (clearnet + tor + i2p)

Classpath of module (Alt+O)
```
bisq.seed.main
```

Main Class (Alt+C)
```
bisq.seed.SeedMain
```

Program Arguments (Alt+R)
```
--appName=bisq2_seed1
```

VM Options (Alt+V)
```
-Dbisq.networkServiceConfig.defaultNodePortByTransportType.clear=8000 
-Dbisq.networkServiceConfig.defaultNodePortByTransportType.tor=1000 
-Dbisq.networkServiceConfig.defaultNodePortByTransportType.i2p=5000 
-Dbisq.networkServiceConfig.supportedTransportTypes.0=TOR 
-Dbisq.networkServiceConfig.supportedTransportTypes.1=I2P 
-Dbisq.networkServiceConfig.supportedTransportTypes.2=CLEAR 
-Dbisq.networkServiceConfig.seedAddressByTransportType.clear.0=127.0.0.1:8000 
-Dbisq.networkServiceConfig.seedAddressByTransportType.clear.1=127.0.0.1:8001
```

#### Run Config: `Seed_2` (clearnet + tor + i2p)

Copy the `Seed_1` run configuration, rename it to `Seed_2` and change:
- Program Arguments to `--appName=bisq2_seed2`
- VM Options: adjust the ports in the following lines to
```
-Dbisq.networkServiceConfig.defaultNodePortByTransportType.clear=8001 
-Dbisq.networkServiceConfig.defaultNodePortByTransportType.tor=1001 
-Dbisq.networkServiceConfig.defaultNodePortByTransportType.i2p=5001 
```


#### Run Config: `Alice_clear`

Classpath of module (Alt+O)
```
bisq.desktop.main
```

Main Class (Alt+C)
```
bisq.desktop.Main
```

Program Arguments (Alt+R)
```
--appName=bisq_Alice
```

VM Options (Alt+V)
```
--add-opens java.base/java.lang.reflect=ALL-UNNAMED 
-Dbisq.networkServiceConfig.supportedTransportTypes.0=CLEAR 
-Dbisq.networkServiceConfig.seedAddressByTransportType.clear.0=127.0.0.1:8000 
-Dbisq.networkServiceConfig.seedAddressByTransportType.clear.1=127.0.0.1:8001
```


#### Run Config: `Alice_tor`

Copy the `Alice_clear` run configuration, rename it to `Alice_tor` and change:
- Program Arguments to `--appName=bisq_Alice_tor`
- VM options to:
```
--add-opens java.base/java.lang.reflect=ALL-UNNAMED 
-Dbisq.networkServiceConfig.supportedTransportTypes.0=TOR 
-Dbisq.networkServiceConfig.seedAddressByTransportType.tor.0=<onion address of seed 1>:1000 
-Dbisq.networkServiceConfig.seedAddressByTransportType.tor.1=<onion address of seed 2>:1001
```

#### Run Config: `Alice_i2p`

Copy the `Alice_clear` run configuration, rename it to `Alice_i2p` and change:
- Program Arguments to `--appName=bisq_Alice_i2p`
- VM options to:
```
--add-opens java.base/java.lang.reflect=ALL-UNNAMED 
-Dbisq.networkServiceConfig.supportedTransportTypes.0=I2P 
-Dbisq.networkServiceConfig.seedAddressByTransportType.i2p.0=<i2p destination of seed 1>:5000 
-Dbisq.networkServiceConfig.seedAddressByTransportType.i2p.1=<i2p destination of seed 2>:5001
```

### IntelliJ IDEA: Gradle run configs

* Create a new IntelliJ IDEA run config of type Gradle
* Choose a config name (e.g. `[gradle] Alice I2P`)
* Choose Gradle Project as `bisq2:desktop`
* Add VM options as necessary, in the format `-Dprop=value` (e.g. `-Dbisq.application.appName=bisq_Alice_i2p`)


### Command line: Gradle run configs

Start a default desktop client with:

```
./gradlew desktop:run
```

Start a customized desktop client with:

```
./gradlew desktop:run \
    -Dbisq.application.appName=bisq_Alice_i2p \
    -Dbisq.networkServiceConfig.supportedTransportTypes.0=I2P \
    -Dbisq.networkServiceConfig.seedAddressByTransportType.i2p.0=<i2p destination of seed 1>:5000 \
    -Dbisq.networkServiceConfig.seedAddressByTransportType.i2p.1=<i2p destination of seed 2>:5001
```