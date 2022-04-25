## Install seed service

```
./seed-debian-install.sh
```

Creates a user `bisq` and installs the service under that username.

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