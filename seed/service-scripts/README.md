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
-Dbisq.networkServiceConfig.seedAddressByTransportType.clear.0=159.65.117.67:8001
-Dbisq.networkServiceConfig.seedAddressByTransportType.tor.0=vg5su3rkksuzsmel2gwpgov6a3azgcmnnad7euophaqd2fnpljx2zayd.onion:1001
-Dbisq.networkServiceConfig.seedAddressByTransportType.i2p.0=u~EXMqCbYcdPHvb7nl-Y3eHxSUbaFhwQLycOtA0c45mhrieMaEbRVSRxaUEtjhgk8nVBpKYiDn4Za6X82aPokSFqURJx09bfKTExTklI~1u~0PJk6Wt3~Jpg4TLCYxql0WEphbEs5oEIR1d4myIm4ng3Iz9TM3dZUBMf4B~oRUiMGRxO-U7Vwxb3Qh1J0ZiqvQZmKzk9~ShEpk-FDR1-j0hlICQ2~RHNM7z4CdWReZLiyY8UboOxkakSIYasVEL2xs2Vgt7t4o078X5AcVtEJu6H31WXvUZSffFrt1BXZNTIoYs1FCCuhS1jMLh8N96eR3AqZ43Nr4Ljp78iqbLdikeVhb53Nzr0rDSYcfh57d2YVitjhfz2ant~6~SGSPxdJRdmsmDkTn5VAZwJhHGM5nh2BQbEwuEeeoufw6s7FNEoWMcv86h6ODmKTO0xyk8oMBT81zjdT8Xg5UkaHMSqJ0DnGcrVN4RQ6kOEbT5wtshVjpHgwWiJvOyEcj8XLJLqAAAA:5001
"
```

If this is seed 2, set the file content to:

```
JAVA_OPTS="
-Dbisq.application.appName=bisq2_seed2
-Dbisq.networkServiceConfig.defaultNodePortByTransportType.clear=8001
-Dbisq.networkServiceConfig.defaultNodePortByTransportType.tor=1001
-Dbisq.networkServiceConfig.defaultNodePortByTransportType.i2p=5001
-Dbisq.networkServiceConfig.seedAddressByTransportType.clear.0=167.71.33.219:8000
-Dbisq.networkServiceConfig.seedAddressByTransportType.tor.0=fyfy4xvqkh46gwbf3d5yi6bszisnz5uqzofgdzx2dr4jv5svrbfhuvad.onion:1000
-Dbisq.networkServiceConfig.seedAddressByTransportType.i2p.0=kglZCQYj~nyK3YlXCD5FjxOY2ggH8yosII0rqc7oqFhFfjKWy-89WYw-~mtTUqzCaN6LGd17XzheKG44XJnKrM-WvP732V8lbJcoMBIKeeHPlcfwpsTNbMJyWeXIlJByYNlw1HPVRMpBtzfJ9IznyQdwQWDkzA72pLreqpzJrgIoVYzP9OTXVLdROXnTP9RdmnzZ0h1B8XhQM-8LjHB7cE9o9VT9IXIFScICM8VZ8I1sp02rn26McTM~~XO5Zs1Df3IMV0eqteAe6TvH~Rc-6Hh3YhPrjEcv-YvV6RUlsoj605mmSO0Sj5oeacH3Cec73BlNJEGfQkmbTrXVNLqt2S4smqmkAhMq~sdCJCRKP8CFeBk6r-qVREucTeW3AmwXuGS~-8s7pAm99SlpTSepp75a2WNTIsWw~rWiHlM6faTJrkjcO5wJM7~G0tdYgVGk4zrt4VJ02AakUdh8wG1Y5sAX-daTUum~0YTk-fIAVBJSEiNc93XgZkwuTcc4J2BqAAAA:5000
"
```

These settings reflect those in
- `application/src/main/resources/Seed.conf` > `seedAddressByTransportType`
- `application/src/main/resources/Bisq.conf` > `seedAddressByTransportType`

Note that both configs explicitly indicate the other seed's addresses and by extension, imply there is only one
`seedAddressByTransportType` per transport type. This helps each seed avoid trying to connect to itself, as it would 
normally do, because by default it would try to connect to all `seedAddressByTransportType`s listed in `Seed.conf`.

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