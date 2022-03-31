# Wallet Regtest Setup

In Bisq 2, we don't implement our own wallet. We rely on external wallets by making RPC calls to them. At the moment, we
support Bitcoin Core and Elements Core.

To help developers set up their environment, we provide a couple Gradle tasks.

**Note**: All files created by regtest instances are in `build/regtest`.

## Bitcoin Core

You can start Bisq 2 with 50 BTC by running:

```
./gradlew :desktopapp:runWithBitcoindRegtestWallet
```

This command starts Bitcoin-Qt, creates a new wallet, mines the initial regtest blocks (101), and starts Bisq 2 with the
wallet.

See: https://developer.bitcoin.org/examples/testing.html

Other tasks to make you life easier:

- `./gradlew bitcoindStart`
- `./gradlew bitcoindStop`
- `./gradlew cleanBitcoindRegtest`

## Elements Core

You can start Bisq 2 with ~20 L-BTC by running:

```
./gradlew :desktopapp:runWithElementsRegtestWallet
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

### Manual BTC Pegin

The `:desktopapp:runWithElementsRegtestWallet` Gradle task does a 20 BTC pegin. Here's a quick summary of how to do it
manually. First, we need to get a pegin address.

```
elements-cli -chain=elementsregtest rpcwallet=$walletPath getpeginaddress
```

Response:

```
{
    "mainchain_address": "2N3i4C56DiqfpdcAJsAdZd2xYpCQMRAroye",
    "claim_script": "0014b515db1688fa148308eb723e00ff8f7913fdcfdb"
}
```

We have to send the BTC we want to convert to L-BTC to the mainchain_address and note the transaction id. Example:
```
bitcoin-cli -regtest -rpcwallet=$walletPath sendtoaddress 2N3i4C56DiqfpdcAJsAdZd2xYpCQMRAroye 20
```

The mainchain transaction needs 102 confirmations before we can get our L-BTC.
```
bitcoin-cli -regtest -rpcwallet=$walletPath -generate 102
```

For the claimpegin RPC call, we need the raw transaction and its txoutproof.

```
bitcoin-cli -regtest getrawtransaction $txId
bitcoin-cli -regtest gettxoutproof [\"$txId\"]
```

Finally, we can claim the pegin.

```
elements-cli -chain=elementsregtest -rpcwallet=$walletPath -generate 1
elements-cli -chain=elementsregtest rpcwallet=$walletPath claimpegin $rawTransaction $txOutProof
elements-cli -chain=elementsregtest -rpcwallet=$walletPath -generate 1
```