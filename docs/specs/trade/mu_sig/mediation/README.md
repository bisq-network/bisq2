# MuSig mediation specifications

Status: **Proposed target behavior; not yet fully implemented.**

The mediated custom payout integration is tracked by
[bisq-network/bisq2#4888](https://github.com/bisq-network/bisq2/issues/4888). The custom payout RPC definitions are
already present, but the complete Java flow and the required MuSig service behavior are not.

The specification is split by concern:

* [Mediated custom payout settlement](custom-payout-settlement.md) defines the domain flow, prerequisites, state
  boundaries, payout rules, and first-version failure behavior.
* [Mediated custom payout interface](custom-payout-interface.md) defines the Java, MuSig service, and P2P responsibilities
  and the contract at their boundaries.

The package-local
[trader mediation specification](../../../../../trade/src/main/java/bisq/trade/mu_sig/mediation/specification.md)
describes the current Java mediation implementation. It remains separate because the target behavior in these documents
has not yet been implemented.
