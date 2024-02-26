## Install I2P service

```
sudo apt install i2p
```

Edit the file `/etc/default/i2p` and modify the following properties to

```
RUN_DAEMON="true"
ALLOW_ROOT="false"
RUN_AS_USER="i2psvc"
```

Note: do not change or remove other lines in the config, but only modify the three mentioned above.

Then start using the service with:

```
# Enable the service
sudo systemctl enable i2p

# Start the service
sudo systemctl start i2p

# Check the system status
sudo systemctl status i2p
```

Note: the official instructions from https://geti2p.net/en/download/debian are slightly outdated and still require
editing the config as described above.

## Install oracle service

### Part 1: Install service

```
./oracle-debian-install.sh
```

Creates a user `bisq2` and installs the service under that username.

### Part 2: Configure service

Edit the file `/etc/default/bisq2-oracle.env` to set the oracle arguments.

```
JAVA_OPTS="
-Xms1500M \
-Xmx2000M \
-Dapplication.appName=bisq2-oracle \
-Dapplication.security.keyBundle.defaultTorPrivateKey=[TOR_PRIV_KEY] \
-Dapplication.oracleNode.privateKey=[PRIV_KEY] \
-Dapplication.oracleNode.publicKey=[PUB_KEY] \
-Dapplication.oracleNode.bisq1Bridge.httpService.url=http://localhost:80 \
-Dapplication.oracleNode.profileId=[PROFILE_ID] \
-Dapplication.oracleNode.bondUserName=[USER_NAME] \
-Dapplication.oracleNode.signatureBase64=[SIG] \
-Dapplication.oracleNode.staticPublicKeysProvided=true
"
```

### Part 3: Finalize install

After the contents of `/etc/default/bisq2-oracle.env` have been set, we can start the service:

```
# Reload systemd daemon configuration
sudo systemctl daemon-reload

# Enable bisq2-oracle service
sudo systemctl enable bisq2-oracle

# Start service
sudo systemctl start bisq2-oracle

# Check service log output
sudo journalctl --no-pager --unit bisq2-oracle
```

## Build oracle based on latest version

The latest version is retrieved from the branch `main` of `bisq-network/bisq2`.

```
./oracle-debian-update.sh --main
```

It stops the service, rebuilds it and starts it at the end.

## Build oracle based on PR branch

```
# Example:
./oracle-debian-update.sh --pr alkum pr-branch-123
```

## Control service

```
# To start
sudo systemctl start bisq2-oracle

# To stop
sudo systemctl stop bisq2-oracle

# To restart
sudo systemctl restart bisq2-oracle

# To check the status
sudo systemctl status bisq2-oracle

# To check the log output
sudo journalctl --no-pager --unit bisq2-oracle
```