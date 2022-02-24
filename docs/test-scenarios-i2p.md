# Test Scenarios (I2P)

This document describes a few manual test scenarios with focus on the I2P network.

The requirements to run them are:

 - access to a virtualization environment (create / delete VMs)
 - each VM can run a seed or a desktop client (JDK 16 or higher)
 - each VM can run the makefile tests (see Requirements in `Makefile` header)
 - each VM can optionally start a local I2P router, for some scenarios
 - have a way to copy files between VMs (a good solution is `wormhole`, see [docs](https://github.com/magic-wormhole/magic-wormhole/blob/master/docs/welcome.md))
 

## 1. Local I2P Routers

In these scenarios, each participant connects to the I2P network via a local I2P router.

 - Preparation
   - Create two VMs
   - On each VM:
     - Clone the repo
     - Install a local I2P router
     - Start I2P router, wait until "shared clients" lights green

### 1.1. All participants on same host

VMs used:

- `vm1`: for all participants

All steps below happen on `vm1`.

Test steps:

 - Start 2 I2P seeds
   - `make start-i2p-seeds`
 - Stop them when their destinations are created
 - Start 2 I2P seeds and 2 I2P clients
   - `make start-i2p-full-env`
 - In both clients: wait until 2-3 active connections are established
 - In one client, create a Trade Intent
 - In the other client: check if Trade Intent is visible

### 1.2. Seeds on one host / Clients on separate host

VMs used:

 - `vm1`: for 2 seeds
 - `vm2`: for 2 desktop clients

Test steps:

 - `vm1`: Start 2 I2P seeds
   - `make start-i2p-seeds`
   - When their destinations are created, copy them to root of repo
     - `make seed1-i2p-destination seed2-i2p-destination`
     - This will create 2 new files in the repo, `seed1-i2p-destination` and `seed2-i2p-destination`
     - Copy both files in the repo root on `vm2`
 - `vm2`: Start 2 I2P clients
   - `make start-i2p-clients`
     - Both these clients will connect to the seeds, then find each other as peers
   - In both clients: wait until 2-3 active connections are established
   - For both desktop clients: Under Settings > Network Info, ensure there are 3 connections
   - In one client, create a Trade Intent
   - In the other client: check if Trade Intent is visible


### 1.3. Seeds on one host / Clients on multiple separate hosts

Extend the scenario above with a block for `vm3`, which repeats the same steps as on `vm2`.

This will create 2 (or N) more clients on each new additional VM, connecting to the same 2 seeds.

All trade intents created on one client should be visible on ALL clients from all VMs (`vm2`, `vm3`, etc).


## 2. Hybrid I2P Routers

In these scenarios, different participants have different ways to connect to the I2P network. Some use a
local I2P router, others use an Embedded I2P router.

TODO


## 3. Embedded I2P Routers

In these scenarios, each participant connects to the I2P network via an embedded I2P router.

Since only one router can run per host, there will be one participant per VM.

TODO