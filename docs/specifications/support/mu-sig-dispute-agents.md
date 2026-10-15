# Dispute agents of MuSig trades

This specification defines when a node processes the requests of the dispute agents of MuSig trades: the mediation
requests which a MuSig mediator receives, and the arbitration requests which a MuSig arbitrator receives.

## Rules

1. A node processes MuSig mediation and arbitration requests only in developer mode. Outside developer mode, the services
   of the MuSig mediator and the MuSig arbitrator neither listen for these requests nor process the requests which the
   node received before the services started.
2. The rule applies to every node, also to a node which holds the identity of an authorized mediator or arbitrator.

## Reasons

- MuSig trading runs only in developer mode, so no honest trader outside developer mode sends such a request.
- The checks of these requests are not complete. An arbitrator verifies the mediation result with the key of the
  mediator which the contract of the requester names. It does not check that this mediator is an authorized bonded
  mediator, and the other trader does not confirm the contract. Any peer can therefore create a contract with its own
  traders and mediator, and a node with the identity of an arbitrator would persist the case, create a channel and send
  messages.

## Known limitations

- **Incomplete checks of the requests in developer mode.** In developer mode an arbitrator still accepts a mediation
  result of a mediator which the requester names, without a check of its role, and without a confirmation of the
  contract by the other trader.
  - Why it is not removed now: MuSig is not active outside developer mode, so the gap affects no user of a release.
  - What removes it: the arbitrator requires that the mediator of the contract is an authorized bonded mediator, other
    than the traders and the arbitrator, and the other trader confirms the hash of the contract.
  - Who decides: the developers, before MuSig leaves developer mode.
