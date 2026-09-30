## Specification for the MuSig mediation process

### Scope

This document describes the mediation process from the perspective of the mediator.

In scope:

* receiving and authorizing mediation requests
* opening and storing mediation cases
* receiving dispute case data
* requesting and verifying payment details
* creating, validating, signing, reopening, and closing mediation cases
* calculating and validating mediation payout proposals

Out of scope:

* trader-side mediation behavior
* UI control behavior
* wallet payout transaction construction
* arbitration, except that a mediation result can later be used as input for arbitration

---

### Terminology

* `MuSigMediationRequest` is the message used by a trader to request mediation.
* `MuSigMediationCase` is the mediator-side persisted case for one trade.
* `MuSigMediationResult` is the mediator's proposed resolution for the trade.
* `MuSigMediationStateChangeMessage` informs traders about mediation state changes.
* `requester` is the trader who requested mediation.
* `peer` is the other trader in the trade.
* `maker` and `taker` are the contract roles used for contract signatures and payment account payload hashes.
* `mediator` is the bonded role assigned in the MuSig contract.
* `tradeId` identifies the mediation case.
* `contractHash` binds mediation results to the MuSig contract.
* `mediationResultSignature` is the mediator signature over the mediation result.
* `payoutDistributionType` defines how the proposed payout amounts are calculated.

Requester/peer are message roles.

Maker/taker are contract roles.

The two role pairs must not be treated as equivalent.

---

### Case lifecycle

A mediation case can be in one of these states:

* `OPEN`
* `RE_OPENED`
* `CLOSED`

Rules:

* A valid mediation request creates an `OPEN` case.
* A mediation case is identified by its `tradeId`.
* A duplicate mediation request for an existing `tradeId` is ignored.
* An open case can be closed with a mediation result.
* A closed case can be reopened.
* A reopened case can be closed again only if a mediation result already exists.
* A mediation result cannot be changed once set.
* A mediation result signature cannot be changed once set.

---

### Opening a mediation case

`MuSigMediationRequest` is accepted only if the request is consistent with the trade contract.

Rules:

* The requester must not be banned.
* The requester and peer must match the contract parties.
* The receiver of the mediation request must match the mediator in the contract.
* Unauthorized requests are ignored.
* If a case with the same `tradeId` already exists, the request is ignored.
* A local mediator identity matching the contract mediator must exist.
* A `MuSigMediationCase` is created and persisted.
* An `OPEN` state change message is sent to requester and peer.
* Mediation-opened trade log messages are added for requester and peer.

If the local mediator identity cannot be found, the request is ignored.

Channel setup:

* The mediator uses a MuSig open trade channel for the trade.
* If a channel for the `tradeId` already exists, it is reused.
* If no channel exists, a mediator-side channel is created for the mediator, requester, and peer.
* The channel is marked as using the mediator as dispute agent.
* Chat messages supplied by the mediation request are added to the channel.
* The mediation case is created only after the channel has been set up.

---

### Dispute case data

`MuSigDisputeCaseDataMessage` is accepted only for known mediation cases.

The message is sent by the peer who did not request mediation after that peer receives the mediator's `OPEN` state change.

Purpose:

* report the peer's contract hash to the mediator
* provide the peer's chat history to the mediator
* allow the mediator to detect and record a contract hash mismatch

Rules:

* The message must reference an existing mediation case.
* The sender must be the peer of that case.
* The sender must not be banned.
* Messages from other senders are ignored.
* Banned senders are ignored.
* The reported contract hash is stored in the mediation case.
* If the reported contract hash does not match the mediation request contract, an issue is added to the mediation case.
* Chat messages from the message are added to the trade channel.
* If the case changes, the case is persisted.

---

### Payment details

The mediator can request payment details from both traders.

They are not shared with the mediator by default. They are exchanged only through an explicit request/response flow when the mediator needs them for the dispute case.

Rules for sending requests:

* The mediation case contract must resolve to a local mediator identity.
* If no local mediator identity is found, no request is sent.
* A `MuSigDisputeCasePaymentDetailsRequest` is sent confidentially to requester and peer.

Rules for receiving responses:

* A response must reference an existing mediation case.
* The sender must be the requester or the peer of that case.
* The sender must not be banned.
* Unknown senders are ignored.
* Banned senders are ignored.

Verification:

* The taker account payload is checked against the taker payload hash in the contract.
* The maker account payload is checked against the maker payload hashes in the contract offer options.
* Matching payloads are stored in the mediation case.
* Mismatches are recorded as `MuSigMediationIssue` entries.
* Mismatch issues are attributed to the contract role resolved from the sender profile.
* If payloads or issues changed, the case is persisted.

Payment detail mismatches are recorded as issues.

They are not silently accepted.

---

### Closing a mediation case

Rules:

* If the case already has a different mediation result, the new result is ignored.
* The result used for closing is validated against the case contract.
* If the case has no result or no signature, the mediator signs the result.
* The signed result is stored in the case.
* The case state is set to `CLOSED`.
* If the result, signature, or state changed, the case is persisted.
* A state change message is sent to requester and peer.
* A trade log message is sent to the trade channel.

Immutability:

* Once set, the mediation result cannot be changed.
* Once set, the mediation result signature cannot be changed.

#### Mediation result

Rules:

* The result is bound to the contract by `contractHash`.
* The mediation result reason is required.
* The payout distribution type is required.
* Summary notes are optional.
* Summary notes must not exceed `MuSigMediationResult.MAX_SUMMARY_NOTES_LENGTH`.
* Payout amounts must be present for every payout distribution except `NO_PAYOUT`.
* Payout amounts must be absent for `NO_PAYOUT`.
* Payout amounts must not be negative.
* The result contract hash must match the case contract hash.
* The payout amounts must match the payout distribution type.

#### Payout calculation

Terminology:

* `tradeAmount` is the BTC-side trade amount.
* `buyerSecurityDeposit` is the buyer security deposit in sats.
* `sellerSecurityDeposit` is the seller security deposit in sats.
* `totalPayoutAmount = tradeAmount + buyerSecurityDeposit + sellerSecurityDeposit`.
* `minimumRefundAmount = 5% of tradeAmount`, rounded to sats.
* `payoutAdjustmentPercentage` is normalized, so `0.10` means 10%.

General rules:

* Payout amounts are expressed in sats.
* `NO_PAYOUT` has no buyer payout amount and no seller payout amount.
* `NO_PAYOUT` has no payout adjustment percentage.
* For all non-custom payout distributions, payout amounts must exactly match the resolver-calculated amounts.
* For payout distributions with compensation or penalty, `payoutAdjustmentPercentage` must be present.
* For payout distributions without compensation or penalty, `payoutAdjustmentPercentage` must be absent.
* For `CUSTOM_PAYOUT`, `payoutAdjustmentPercentage` must be absent.
* For `CUSTOM_PAYOUT`, buyer and seller payouts must add up to `totalPayoutAmount`.
* For `CUSTOM_PAYOUT`, both traders must receive at least `minimumRefundAmount`.
* For payout distributions with payout amounts, each trader must receive at least `minimumRefundAmount` to preserve an incentive to participate in mediation.

#### No payout

`NO_PAYOUT` means the mediator does not propose payout amounts.

#### Buyer gets trade amount

`BUYER_GETS_TRADE_AMOUNT` assigns the trade amount to the buyer.

Result:

* Buyer gets `tradeAmount + buyerSecurityDeposit`.
* Seller gets `sellerSecurityDeposit`.

#### Seller gets trade amount

`SELLER_GETS_TRADE_AMOUNT` assigns the trade amount to the seller.

Result:

* Buyer gets `buyerSecurityDeposit`.
* Seller gets `tradeAmount + sellerSecurityDeposit`.

#### Buyer gets trade amount plus compensation

`BUYER_GETS_TRADE_AMOUNT_PLUS_COMPENSATION` starts from the buyer-gets-trade-amount result and transfers additional compensation from seller to buyer.

Base result:

* Buyer base amount is `tradeAmount + buyerSecurityDeposit`.
* Seller base amount is `sellerSecurityDeposit`.

Transfer:

* Requested transfer amount is `tradeAmount * payoutAdjustmentPercentage`, rounded to sats.
* The transfer is capped so the seller keeps at least `minimumRefundAmount`.

Result:

* Buyer gets `buyerBaseAmount + transferAmount`.
* Seller gets `sellerBaseAmount - transferAmount`.

#### Buyer gets trade amount minus penalty

`BUYER_GETS_TRADE_AMOUNT_MINUS_PENALTY` starts from the buyer-gets-trade-amount result and transfers a penalty from buyer to seller.

Base result:

* Buyer base amount is `tradeAmount + buyerSecurityDeposit`.
* Seller base amount is `sellerSecurityDeposit`.

Transfer:

* Requested transfer amount is `tradeAmount * payoutAdjustmentPercentage`, rounded to sats.
* The transfer is capped so the buyer keeps at least `minimumRefundAmount`.

Result:

* Buyer gets `buyerBaseAmount - transferAmount`.
* Seller gets `sellerBaseAmount + transferAmount`.

#### Seller gets trade amount plus compensation

`SELLER_GETS_TRADE_AMOUNT_PLUS_COMPENSATION` starts from the seller-gets-trade-amount result and transfers additional compensation from buyer to seller.

Base result:

* Buyer base amount is `buyerSecurityDeposit`.
* Seller base amount is `tradeAmount + sellerSecurityDeposit`.

Transfer:

* Requested transfer amount is `tradeAmount * payoutAdjustmentPercentage`, rounded to sats.
* The transfer is capped so the buyer keeps at least `minimumRefundAmount`.

Result:

* Buyer gets `buyerBaseAmount - transferAmount`.
* Seller gets `sellerBaseAmount + transferAmount`.

#### Seller gets trade amount minus penalty

`SELLER_GETS_TRADE_AMOUNT_MINUS_PENALTY` starts from the seller-gets-trade-amount result and transfers a penalty from seller to buyer.

Base result:

* Buyer base amount is `buyerSecurityDeposit`.
* Seller base amount is `tradeAmount + sellerSecurityDeposit`.

Transfer:

* Requested transfer amount is `tradeAmount * payoutAdjustmentPercentage`, rounded to sats.
* The transfer is capped so the seller keeps at least `minimumRefundAmount`.

Result:

* Buyer gets `buyerBaseAmount + transferAmount`.
* Seller gets `sellerBaseAmount - transferAmount`.

#### Custom payout

`CUSTOM_PAYOUT` lets the mediator propose buyer and seller payout amounts manually.

Rules:

* Buyer payout amount must be within `[minimumRefundAmount, totalPayoutAmount - minimumRefundAmount]`.
* Seller payout amount must be within `[minimumRefundAmount, totalPayoutAmount - minimumRefundAmount]`.
* Buyer payout amount plus seller payout amount must equal `totalPayoutAmount`.

When resolving custom payout input:

* If the buyer amount is edited, the buyer amount is clamped to the allowed range and the seller amount is derived from the total.
* If the seller amount is edited, the seller amount is clamped to the allowed range and the buyer amount is derived from the total.

#### Payout example

Example values:

* `tradeAmount = 480,000 sats`
* `buyerSecurityDeposit = 120,000 sats`
* `sellerSecurityDeposit = 120,000 sats`
* `totalPayoutAmount = 720,000 sats`
* `minimumRefundAmount = 24,000 sats`

Examples:

* `BUYER_GETS_TRADE_AMOUNT`: buyer gets `600,000`, seller gets `120,000`.
* `SELLER_GETS_TRADE_AMOUNT`: buyer gets `120,000`, seller gets `600,000`.
* `BUYER_GETS_TRADE_AMOUNT_PLUS_COMPENSATION` with 10% adjustment: transfer is `48,000`, buyer gets `648,000`, seller gets `72,000`.
* `BUYER_GETS_TRADE_AMOUNT_PLUS_COMPENSATION` with 50% adjustment: requested transfer is `240,000`, capped to `96,000`, buyer gets `696,000`, seller gets `24,000`.
* `CUSTOM_PAYOUT`: buyer gets `100,000`, seller gets `620,000`.

---

### Reopening and re-closing

Rules:

* The case state is set to `RE_OPENED`.
* If the state changed, the case is persisted.
* A state change message is sent to requester and peer.
* A trade log message is sent to the trade channel.

A reopened case can be closed again.

Rules:

* An existing mediation result must be present.
* The mediation result is not changed.
* The mediation result signature is not changed.
* The case state is set to `CLOSED`.
* If the state changed, the case is persisted and traders are notified.

---

### Removing a mediation case

Removing a mediation case:

* leaves the trade channel
* removes the case from the mediator store
* persists the store

Removing a case is local mediator-side cleanup.

---

### Leaving chat

When the mediator leaves chat:

* the mediation case is marked as `mediatorHasLeftChat`
* the changed case is persisted
* the mediator leaves the trade channel

If no trade channel is found, the missing channel is logged.

---

### Invariants

* A mediation case belongs to exactly one `tradeId`.
* A duplicate mediation request must not create a second case.
* Only contract parties can send follow-up case data.
* The mediator must match the contract mediator.
* The local mediator identity must exist before the mediator opens a case or requests payment details.
* A mediation result contract hash must match the case contract.
* A mediation result cannot be changed once set.
* A mediation result signature cannot be changed once set.
* Payout amounts must match the payout distribution type.
* Invalid payment details are recorded as issues instead of being silently accepted.
