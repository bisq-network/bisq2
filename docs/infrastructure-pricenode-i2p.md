## Expose a pricenode via I2P

This documents how to create a tunnel between an I2P address and an existing public pricenode. The outcome in this
example will be an I2P destination which points to https://price.bisq.wiz.biz .

A similar approach can be used to expose a local pricenode (e.g. if pricenode running on localhost) on the I2P network.

### Steps

Prerequisites

- an I2P installation
- a running pricenode, reachable via clearnet (in this example: https://price.bisq.wiz.biz)

Steps

- Open the I2P Hidden Services Manager (http://127.0.0.1:7657/i2ptunnelmgr)
- Create a new I2P Hidden Service with
    - Name: I2P Pricenode Proxy
    - Settings under the Description section
        - Automatically start tunnel when router starts: yes
    - Settings under the Target section
        - Host: price.bisq.wiz.biz
        - Port: 443
        - Use SSL to connect to target: yes
- Once created, go back to the I2p Hidden Services Manager
    - Start the new tunnel
    - When new tunnel is ready, the Preview button is visible
    - Click Preview to locally open the b32.i2p version of the newly created I2P destination
        - It might take 1-2 minutes until it is reachable
- Done
- To get the full I2P destination of the new tunnel, go to the I2P Hidden Service Manager > click on the newly created
  tunnel > Local destination.

Once the pricenode is available on I2P, remember to add it to the list in `MarketPriceServiceConfigFactory`.