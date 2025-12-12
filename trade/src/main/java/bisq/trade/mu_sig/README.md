### Setup for musig trade protocol

Clone the Bisq MuSig project from https://github.com/bisq-network/bisq-musig, follow the instructions in rpc readme
file.

As long branch https://github.com/stejbac/bisq-musig/tree/add-more-rpc-integration-tests is not merged, use that as that
comes with support for custom ports.
You need to run 2 instances for the 2 trade peers with different ports.

`cargo run --bin musigd -- --port 50090`
`cargo run --bin musigd -- --port 50091`

Start Bisq apps with the distinct port:

Alice:

```
-Dapplication.appName=bisq2_Alice
-Dapplication.network.supportedTransportTypes.2=CLEAR
-Dapplication.devMode=true
-Dapplication.trade.muSig.grpcServer.port=50090
```

Bob:

```
-Dapplication.appName=bisq2_Bob
-Dapplication.network.supportedTransportTypes.2=CLEAR
-Dapplication.devMode=true
-Dapplication.trade.muSig.grpcServer.port=50091
```

Create a sell Bitcoin offer (atm only one protocol pair is implemented).
Let the peer take that offer.
Skip blockchain confirmation. The subscription for getting confirmations is not implemented yet on the rust side.
