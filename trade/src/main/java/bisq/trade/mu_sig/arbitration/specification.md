## Specification for the MuSig trader arbitration process

### Scope

This document describes the arbitration process from the perspective of a trader.

In scope:

* selecting the arbitrator assigned to the trade
* requesting arbitration after mediation
* receiving arbitration state changes
* responding to payment detail requests
* receiving and verifying arbitration results

Out of scope:

* arbitrator-side case handling
* arbitrator payout calculation
* mediator-side case handling
* UI control behavior
* wallet payout transaction construction

---

### Terminology

* `MuSigArbitrationRequest` is the message used by a trader to request arbitration.
* `MuSigArbitrationStateChangeMessage` is the arbitrator message that informs traders about arbitration state changes.
* `MuSigArbitrationResult` is the arbitrator's final resolution for the trade.
* `MuSigMediationResult` is the signed mediation result required to request arbitration.
* `MuSigDisputeCasePaymentDetailsRequest` is the arbitrator request for trader payment details.
* `MuSigDisputeCasePaymentDetailsResponse` is the trader response containing payment details.
* `requester` is the trader who requested arbitration.
* `peer` is the other trader in the trade.
* `maker` and `taker` are the contract roles used for payment account payload hashes.
* `mediator` is the bonded role assigned in the MuSig contract for mediation.
* `arbitrator` is the bonded role assigned in the MuSig contract for arbitration.
* `tradeId` identifies the trade and arbitration case.
* `contractHash` binds mediation and arbitration results to the MuSig contract.
* `mediationResultSignature` is the mediator signature over the mediation result.
* `arbitrationResultSignature` is the arbitrator signature over the arbitration result.

Requester/peer are message roles.

Maker/taker are contract roles.

The two role pairs must not be treated as equivalent.

---

### Dispute lifecycle

A trader-side MuSig dispute can be in one of these arbitration states:

* `ARBITRATION_REQUESTED`
* `ARBITRATION_OPEN`
* `ARBITRATION_CLOSED`

Rules:

* A trader can request arbitration only from `MEDIATION_CLOSED`.
* Requesting arbitration sets the local state to `ARBITRATION_REQUESTED`.
* An arbitrator `OPEN` state change moves the trade to `ARBITRATION_OPEN`.
* An arbitrator `CLOSED` state change moves the trade to `ARBITRATION_CLOSED` only from `ARBITRATION_OPEN`.
* Arbitration messages that arrive before the trade and channel are available are kept pending and processed later.
* Arbitration messages that arrive before the trade is in the required local state are kept pending and processed later.
* A duplicate `OPEN` state change is ignored when the trade is already `ARBITRATION_OPEN`.
* A duplicate `CLOSED` state change is ignored when the trade is already `ARBITRATION_CLOSED`.
* An arbitration result cannot be changed once stored.
* An arbitration result signature cannot be changed once stored.

---

### Selecting an arbitrator

The arbitrator is selected deterministically from authorized bonded arbitrator roles.

Rules:

* The maker and taker cannot be selected as arbitrator.
* The mediator cannot be selected as arbitrator.
* Selection uses the maker profile id, taker profile id, and offer id.
* The selected arbitrator profile must resolve to a known user profile.
* The trader arbitration flow uses the arbitrator assigned in the MuSig contract.

---

### Requesting arbitration

A trader can request arbitration after mediation has closed.

Rules:

* The requesting trader must not be banned.
* The trade contract must define an arbitrator.
* The current dispute state must be `MEDIATION_CLOSED`.
* The MuSig open trade channel must exist.
* A mediation result must already be stored.
* A mediation result signature must already be stored.
* The local dispute state is set to `ARBITRATION_REQUESTED`.
* The trade is persisted before the arbitration request is sent.
* The trade channel is marked as using the arbitrator as dispute agent.
* An arbitration-requested trade log message is added to the channel.
* Chat messages from the trade channel are included in the arbitration request according to the chat history pruning rules.
* The arbitration request includes the mediation result and mediation result signature.
* A `MuSigArbitrationRequest` is sent confidentially to the arbitrator.

---

### Chat history pruning

Arbitration requests can include trade chat history for dispute context.

Rules:

* Chat history is taken from the MuSig open trade channel.
* If the full chat history would make the message too large, older messages are removed.
* Newer messages are kept before older messages.
* The message is retried with fewer chat messages until it fits the serialized size limit.
* If the message still exceeds the serialized size limit after all chat messages are removed, it is sent without chat history or rejected with a logged error.
* If chat messages are pruned, the pruning is logged.

---

### Opening arbitration

The arbitrator opens arbitration by sending an `OPEN` state change message.

Rules:

* The message must reference an existing trade and channel.
* The trade contract must define an arbitrator.
* The sender must be the arbitrator from the trade contract.
* The sender must not be banned.
* Messages from other senders are ignored.
* If the trader requested arbitration, `ARBITRATION_REQUESTED` becomes `ARBITRATION_OPEN`.
* If the peer requested arbitration, `MEDIATION_CLOSED` becomes `ARBITRATION_OPEN`.
* Duplicate `OPEN` messages are ignored.
* The trade is persisted when the dispute state changes.
* The trade channel is updated to use the arbitrator as dispute agent.
* An arbitration-opened trade log message is added to the channel.

---

### Payment details

The arbitrator can request payment details from both traders.

They are not shared with the arbitrator by default. They are exchanged only through an explicit request/response flow when the arbitrator needs them for the dispute case.

Rules for receiving requests:

* The message must reference an existing trade and channel.
* The sender must be the arbitrator from the trade contract.
* The sender must not be banned.
* Requests from other senders are ignored.
* Requests from banned senders are ignored.
* If the trade is not yet in an arbitration state, the request is kept pending.

Rules for sending responses:

* The trader must have both maker and taker payment account payloads available.
* If payment account payloads are incomplete, no response is sent.
* The response contains the taker account payload and maker account payload.
* The response is sent confidentially to the arbitrator.

---

### Closing arbitration

The arbitrator closes arbitration by sending a `CLOSED` state change message.

Rules:

* The current dispute state must be `ARBITRATION_OPEN`.
* The message must contain an arbitration result.
* The message must contain an arbitration result signature.
* The arbitration result contract hash must match the trade contract.
* The arbitration result signature must verify against the arbitrator public key from the contract.
* Messages without a result or signature are ignored.
* Messages with an invalid result signature are ignored.
* If no arbitration result is stored yet, the result and signature are stored.
* If a different result or signature is received after one is already stored, the changed result or signature is ignored.
* The dispute state is set to `ARBITRATION_CLOSED`.
* The trade is persisted when the dispute state changes.
* The trade channel keeps the arbitrator as dispute agent.

---

### Invariants

* An arbitration request belongs to exactly one `tradeId`.
* A trader can request arbitration only after mediation has closed.
* A signed mediation result must be available before arbitration can be requested.
* Only the contract arbitrator can send arbitration state changes.
* The arbitrator must not be banned.
* Payment details are sent only after an explicit arbitrator request.
* A stored arbitration result cannot be changed.
* A stored arbitration result signature cannot be changed.
* An arbitration result must verify against the arbitrator public key from the contract.
* Arbitration state changes are applied only through the arbitration dispute states.
