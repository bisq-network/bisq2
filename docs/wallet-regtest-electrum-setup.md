# Electrum Wallet Regtest Setup

## Setup
1. Install Bitcoin Core (https://bitcoin.org/en/download)
2. Install ElectrumX (https://electrumx-spesmilo.readthedocs.io/en/latest/HOWTO.html#running)
3. Install Electrum (https://electrum.org/#download)

## Running Electrum in Regtest Mode
1. Start Bitcoin Core in Regtest Mode
```
bitcoin-qt -regtest -daemon -server -rpcbind -rpcallowip=127.0.0.1 -rpcuser=bisq -rpcpassword=bisq -fallbackfee=0.00000001 -whitelist=127.0.0.1 -txindex=1
```
2. Start ElectrumX and connect it to Bitcoin Core (Replace <DB_DIRECTORY> by a path)
```
SERVICES=tcp://:50001,rpc:// COIN=Bitcoin NET=regtest DAEMON_URL=http://bisq:bisq@localhost:18443 DB_DIRECTORY=<DB_DIRECTORY> ./electrumx_server
```
3. Start the Electrum daemon in Regtest Mode and connect it to ElectrumX
```
electrum --regtest daemon -s "localhost:50001:t" -v
```
4. Verify connection by running
```
electrum --regtest getinfo
```
Now you can run Electrum commands with
```
electrum --regtest <command>
```