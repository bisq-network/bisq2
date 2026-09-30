## Specification for the MuSig arbitration process

### Scope

This document describes the arbitration process from the perspective of the arbitrator.

In scope:

* receiving and authorizing arbitration requests
* opening and storing arbitration cases
* verifying the signed mediation result used to request arbitration
* requesting and verifying payment details
* creating, validating, signing, and closing arbitration cases
* calculating and validating arbitration payouts

Out of scope:

* trader-side arbitration behavior
* mediator-side behavior before arbitration is requested
* UI control behavior
* wallet payout transaction construction

---

### Terminology

* `MuSigArbitrationRequest` is the message used by a trader to request arbitration.
* `MuSigArbitrationCase` is the arbitrator-side persisted case for one trade.
* `MuSigMediationResult` is the signed mediation result required to request escalation to arbitration.
* `MuSigArbitrationResult` is the arbitrator's final resolution for the trade.
* `MuSigArbitrationStateChangeMessage` informs traders about arbitration state changes.
* `requester` is the trader who requested arbitration.
* `peer` is the other trader in the trade.
* `maker` and `taker` are the contract roles used for payment account payload hashes.
* `mediator` is the bonded role assigned in the MuSig contract for mediation.
* `arbitrator` is the bonded role assigned in the MuSig contract for arbitration.
* `tradeId` identifies the arbitration case.
* `contractHash` binds mediation and arbitration results to the MuSig contract.
* `mediationResultSignature` is the mediator signature over the mediation result.
* `arbitrationResultSignature` is the arbitrator signature over the arbitration result.
* `payoutDistributionType` defines how the final payout amounts are calculated.

Requester/peer are message roles.

Maker/taker are contract roles.

The two role pairs must not be treated as equivalent.

---

### Case lifecycle

An arbitration case can be in one of these states:

* `OPEN`
* `CLOSED`

Rules:

* A valid arbitration request creates an `OPEN` case.
* An arbitration case is identified by its `tradeId`.
* A duplicate arbitration request for an existing `tradeId` is ignored.
* An open case can be closed with an arbitration result.
* An arbitration result cannot be changed once set.
* An arbitration result signature cannot be changed once set.

---

### Opening an arbitration case

`MuSigArbitrationRequest` is accepted only if the request is consistent with the trade contract and contains a valid signed mediation result.

Rules:

* The requester must not be banned.
* The requester and peer must match the contract parties.
* The receiver of the arbitration request must match the arbitrator in the contract.
* Unauthorized requests are ignored.
* If a case with the same `tradeId` already exists, the request is ignored.
* The mediation result contract hash must match the arbitration request contract.
* The arbitration request contract must define a mediator.
* The mediation result signature must verify against the mediator public key from the contract.
* Requests with an invalid mediation result or invalid mediation result signature are ignored.
* A local arbitrator identity matching the contract arbitrator must exist.
* A `MuSigArbitrationCase` is created and persisted.
* An `OPEN` state change message is sent to requester and peer.
* Arbitration-opened trade log messages are added for requester and peer.

If the local arbitrator identity cannot be found, the request is ignored.

Channel setup:

* The arbitrator uses a MuSig open trade channel for the trade.
* If a channel for the `tradeId` already exists, it is reused.
* If no channel exists, an arbitrator-side channel is created for the arbitrator, requester, and peer.
* The channel is marked as using the arbitrator as dispute agent.
* Chat messages supplied by the arbitration request are added to the channel.
* The arbitration case is created only after the channel has been set up.

---

### Payment details

The arbitrator can request payment details from both traders.

They are not shared with the arbitrator by default. They are exchanged only through an explicit request/response flow when the arbitrator needs them for the dispute case.

Rules for sending requests:

* The arbitration case contract must resolve to a local arbitrator identity.
* If no local arbitrator identity is found, no request is sent.
* A `MuSigDisputeCasePaymentDetailsRequest` is sent confidentially to requester and peer.

Rules for receiving responses:

* A response must reference an existing arbitration case.
* The sender must be the requester or the peer of that case.
* The sender must not be banned.
* Unknown senders are ignored.
* Banned senders are ignored.

Verification:

* The taker account payload is checked against the taker payload hash in the contract.
* The maker account payload is checked against the maker payload hashes in the contract offer options.
* Matching payloads are stored in the arbitration case.
* Mismatches are recorded as `MuSigArbitrationIssue` entries.
* Mismatch issues are attributed to the contract role resolved from the sender profile.
* If payloads or issues changed, the case is persisted.

Payment detail mismatches are recorded as issues.

They are not silently accepted.

---

### Closing an arbitration case

Rules:

* If the case already has a different arbitration result, the new result is ignored.
* The result used for closing is validated against the case contract.
* If the case has no result or no signature, the arbitrator signs the result.
* The signed result is stored in the case.
* The case state is set to `CLOSED`.
* If the result, signature, or state changed, the case is persisted.
* A state change message is sent to requester and peer.
* A trade log message is sent to the trade channel.

Immutability:

* Once set, the arbitration result cannot be changed.
* Once set, the arbitration result signature cannot be changed.

#### Arbitration result

Rules:

* The result is bound to the contract by `contractHash`.
* The arbitration result reason is required.
* The payout distribution type is required.
* Summary notes are optional.
* Summary notes must not exceed `MuSigArbitrationResult.MAX_SUMMARY_NOTES_LENGTH`.
* Payout amounts must not be negative.
* The result contract hash must match the case contract hash.
* The payout amounts must match the payout distribution type.

#### Payout calculation

Terminology:

* `tradeAmount` is the BTC-side trade amount.
* `buyerSecurityDeposit` is the buyer security deposit in sats.
* `sellerSecurityDeposit` is the seller security deposit in sats.
* `totalPayoutAmount = tradeAmount + buyerSecurityDeposit + sellerSecurityDeposit`.

General rules:

* Payout amounts are expressed in sats.
* For all non-custom payout distributions, payout amounts must exactly match the resolver-calculated amounts.
* For `CUSTOM_PAYOUT`, combined buyer and seller payouts must not exceed `totalPayoutAmount`.
* For `CUSTOM_PAYOUT`, buyer and seller payouts may add up to less than `totalPayoutAmount`.
* Arbitration payout rules do not require each trader to receive a minimum refund amount.

#### Buyer gets trade amount

`BUYER_GETS_TRADE_AMOUNT` assigns the trade amount to the buyer and pays no amount to the seller.

Result:

* Buyer gets `tradeAmount + buyerSecurityDeposit`.
* Seller gets `0`.

#### Seller gets trade amount

`SELLER_GETS_TRADE_AMOUNT` assigns the trade amount to the seller and pays no amount to the buyer.

Result:

* Buyer gets `0`.
* Seller gets `tradeAmount + sellerSecurityDeposit`.

#### Custom payout

`CUSTOM_PAYOUT` allows explicit buyer and seller payout amounts.

Rules:

* The combined buyer and seller payout amount must not exceed `totalPayoutAmount`.
* The custom payout can leave part of `totalPayoutAmount` undistributed.

When custom payout amounts are resolved from editable inputs:

* Each amount is bounded to the range `0..totalPayoutAmount`.
* If the sum exceeds `totalPayoutAmount`, the non-edited side is reduced by the excess amount.

---

### Removing an arbitration case

Removing an arbitration case:

* leaves the trade channel
* removes the case from the arbitrator store
* persists the store

Removing a case is local arbitrator-side cleanup.

---

### Leaving chat

When the arbitrator leaves chat:

* the arbitration case is marked as `arbitratorHasLeftChat`
* the changed case is persisted
* the arbitrator leaves the trade channel

If no trade channel is found, the missing channel is logged.

---

### Invariants

* An arbitration case belongs to exactly one `tradeId`.
* A duplicate arbitration request must not create a second case.
* Only requester and peer can send payment detail responses.
* The arbitrator must match the contract arbitrator.
* The local arbitrator identity must exist before the arbitrator opens a case or requests payment details.
* The mediation result contract hash must match the arbitration request contract.
* The mediation result signature must verify against the mediator public key from the contract.
* An arbitration result contract hash must match the case contract.
* An arbitration result cannot be changed once set.
* An arbitration result signature cannot be changed once set.
* Payout amounts must match the payout distribution type.
* Invalid payment details are recorded as issues instead of being silently accepted.
