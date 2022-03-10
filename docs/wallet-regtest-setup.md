# Wallet Regtest Setup
In Bisq 2, we don't implement our own wallet. We rely on external wallets by making RPC calls to them.  At the moment, 
we support Bitcoin Core and Elements Core. 

To help developers set up their environment, we provide a couple Gradle tasks.

**Note**: All files created by regtest instances are in `build/regtest`.

## Bitcoin Core
You can start Bisq 2 with 50 BTC by running:
```
./gradlew :desktop:runWithBitcoindRegtestWallet
```
This command starts Bitcoin-Qt, creates a new wallet, mines the initial regtest blocks (101), and starts Bisq 2 with
the wallet.

See: https://developer.bitcoin.org/examples/testing.html

Other tasks to make you life easier:
- `./gradlew bitcoindStart`
- `./gradlew bitcoindStop`
- `./gradlew cleanBitcoindRegtest`

## Elements Core
You can start Bisq 2 with ~20 L-BTC by running:
```
./gradlew :desktop:runWithElementsRegtestWallet
```
This command starts Bitcoin-Qt, creates a new wallet, mines the initial regtest blocks (101), starts Elements-Qt, pegs 
in 20 BTC to get 20 L-BTC, and starts Bisq 2 with the wallet.

See: https://docs.blockstream.com/liquid/technical_overview.html

Other tasks to make you life easier:
- `./gradlew elementsdStart`
- `./gradlew elementsdStop`
- `./gradlew cleanElementsdRegtest`


- `./gradlew bitcoindStartForElementsRegtest`
- `./gradlew bitcoindStopForElementsRegtest`
- `./gradlew cleanBitcoindRegtestForElementsRegtest`
