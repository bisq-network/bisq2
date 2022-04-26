## Install seed service

### Part 1: Install service

```
./seed-debian-install.sh
```

Creates a user `bisq` and installs the service under that username.

### Part 2: Configure service

Edit the file `/etc/default/bisq2-seed.env` to set the seed arguments.

If this is seed 1, ensure the content of that file now reads:

```
JAVA_OPTS="
-Dbisq.application.appName=bisq2_seed1
-Dbisq.networkServiceConfig.defaultNodePortByTransportType.clear=8000
-Dbisq.networkServiceConfig.defaultNodePortByTransportType.tor=1000
-Dbisq.networkServiceConfig.defaultNodePortByTransportType.i2p=5000
"
```

If this is seed 2, set the file content to:

```
JAVA_OPTS="
-Dbisq.application.appName=bisq2_seed2
-Dbisq.networkServiceConfig.defaultNodePortByTransportType.clear=8001
-Dbisq.networkServiceConfig.defaultNodePortByTransportType.tor=1001
-Dbisq.networkServiceConfig.defaultNodePortByTransportType.i2p=5001
"
```

These settings reflect those in
- `application/src/main/resources/Seed.conf` > `seedAddressByTransportType`
- `application/src/main/resources/Bisq.conf` > `seedAddressByTransportType`

### Part 3: Finalize install

After the contents of `/etc/default/bisq2-seed.env` have been set, we can start the service:

```
# Reload systemd daemon configuration
sudo systemctl daemon-reload

# Enable bisq2-seed service
sudo systemctl enable bisq2-seed

# Start service
sudo systemctl start bisq2-seed

# Check service log output
sudo journalctl --no-pager --unit bisq2-seed
```


## Build seed based on latest version

The latest version is retrieved from the branch `main` of `bisq-network/bisq2`.
```
./seed-debian-update.sh --main
```

It stops the service, rebuilds it and starts it at the end.


## Build seed based on PR branch

```
# Example:
./seed-debian-update.sh --pr alkum pr-branch-123
```

## Control service

```
# To start
sudo systemctl start bisq2-seed

# To stop
sudo systemctl stop bisq2-seed

# To restart
sudo systemctl restart bisq2-seed

# To check the status
sudo systemctl status bisq2-seed

# To check the log output
sudo journalctl --no-pager --unit bisq2-seed
```