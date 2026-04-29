## Specification for the MuSig trader mediation process

### Scope

This document describes the mediation process from the perspective of a trader.

In scope:

* selecting the mediator assigned to the trade
* requesting mediation
* receiving mediation state changes
* sending dispute case data
* responding to payment detail requests
* receiving, verifying, accepting, and rejecting mediation results

Out of scope:

* mediator-side case handling
* mediator payout calculation
* UI control behavior
* wallet payout transaction construction
* arbitration, except that a signed mediation result is later used as input for arbitration

---

### Terminology

* `MuSigMediationRequest` is the message used by a trader to request mediation.
* `MuSigMediationStateChangeMessage` is the mediator message that informs traders about mediation state changes.
* `MuSigMediationResult` is the mediator's proposed resolution for the trade.
* `MuSigMediationResultAcceptanceMessage` is the peer-to-peer message used by traders to communicate whether they accept the mediation result.
* `MuSigDisputeCaseDataMessage` is the message sent by the non-requesting peer after mediation is opened.
* `MuSigDisputeCasePaymentDetailsRequest` is the mediator request for trader payment details.
* `MuSigDisputeCasePaymentDetailsResponse` is the trader response containing payment details.
* `requester` is the trader who requested mediation.
* `peer` is the other trader in the trade.
* `maker` and `taker` are the contract roles used for payment account payload hashes.
* `mediator` is the bonded role assigned in the MuSig contract.
* `tradeId` identifies the trade and mediation case.
* `contractHash` binds mediation results to the MuSig contract.
* `mediationResultSignature` is the mediator signature over the mediation result.

Requester/peer are message roles.

Maker/taker are contract roles.

The two role pairs must not be treated as equivalent.

---

### Dispute lifecycle

A trader-side MuSig dispute can be in one of these mediation states:

* `NO_DISPUTE`
* `MEDIATION_REQUESTED`
* `MEDIATION_OPEN`
* `MEDIATION_CLOSED`
* `MEDIATION_RE_OPENED`

Rules:

* A trader can request mediation only from `NO_DISPUTE`.
* Requesting mediation sets the local state to `MEDIATION_REQUESTED`.
* A mediator `OPEN` state change moves the trade to `MEDIATION_OPEN`.
* A mediator `CLOSED` state change moves the trade to `MEDIATION_CLOSED` only from `MEDIATION_OPEN` or `MEDIATION_RE_OPENED`.
* A mediator `RE_OPENED` state change moves the trade to `MEDIATION_RE_OPENED` only from `MEDIATION_CLOSED`.
* Mediation messages that arrive before the trade and channel are available are kept pending and processed later.
* Mediation messages that arrive before the trade is in the required local state are kept pending and processed later.
* Mediation state changes are ignored once the trade is in arbitration.
* A mediation result cannot be changed once stored.
* A mediation result signature cannot be changed once stored.

---

### Selecting a mediator

The mediator is selected deterministically from authorized bonded mediator roles.

Rules:

* The maker and taker cannot be selected as mediator.
* Selection uses the maker profile id, taker profile id, and offer id.
* The selected mediator profile must resolve to a known user profile.
* The trader mediation flow uses the mediator assigned in the MuSig contract.

---

### Requesting mediation

A trader can request mediation when the trade has a mediator and no dispute is open.

Rules:

* The requesting trader must not be banned.
* The trade contract must define a mediator.
* The current dispute state must be `NO_DISPUTE`.
* The MuSig open trade channel must exist.
* The local dispute state is set to `MEDIATION_REQUESTED`.
* The trade is persisted before the mediation request is sent.
* The trade channel is marked as using the mediator as dispute agent.
* A mediation-requested trade log message is added to the channel.
* Chat messages from the trade channel are included in the mediation request according to the chat history pruning rules.
* A `MuSigMediationRequest` is sent confidentially to the mediator.

---

### Chat history pruning

Mediation messages can include trade chat history for dispute context.

Rules:

* Chat history is taken from the MuSig open trade channel.
* If the full chat history would make the message too large, older messages are removed.
* Newer messages are kept before older messages.
* The message is retried with fewer chat messages until it fits the serialized size limit.
* If the message still exceeds the serialized size limit after all chat messages are removed, it is sent without chat history or rejected with a logged error.
* If chat messages are pruned, the pruning is logged.

---

### Opening mediation

The mediator opens mediation by sending an `OPEN` state change message.

Rules:

* The message must reference an existing trade and channel.
* The trade contract must define a mediator.
* The sender must be the mediator from the trade contract.
* The sender must not be banned.
* Messages from other senders are ignored.
* If the trader requested mediation, `MEDIATION_REQUESTED` becomes `MEDIATION_OPEN`.
* If the peer requested mediation, `NO_DISPUTE` becomes `MEDIATION_OPEN`.
* Duplicate `OPEN` messages are ignored.
* The trade is persisted when the dispute state changes.
* The trade channel is updated to use the mediator as dispute agent.
* A mediation-opened trade log message is added to the channel.

---

### Dispute case data

The trader who did not request mediation sends `MuSigDisputeCaseDataMessage` after receiving the mediator's `OPEN` state change.

Purpose:

* report the trader's contract hash to the mediator
* provide the trader's chat history to the mediator
* allow the mediator to detect a contract hash mismatch

Rules:

* The message is sent only by the non-requesting peer.
* The message is sent confidentially to the mediator.
* The reported contract hash is calculated from the trader's local contract.
* Chat messages from the trade channel are included according to the chat history pruning rules.
* If no trade channel is found, the message is sent with an empty chat history.

---

### Payment details

The mediator can request payment details from both traders.

They are not shared with the mediator by default. They are exchanged only through an explicit request/response flow when the mediator needs them for the dispute case.

Rules for receiving requests:

* The message must reference an existing trade and channel.
* The sender must be the mediator from the trade contract.
* The sender must not be banned.
* Requests from other senders are ignored.
* Requests from banned senders are ignored.
* If the trade is already in arbitration, mediator requests are ignored.
* If the trade is not yet in a mediation state, the request is kept pending.

Rules for sending responses:

* The trader must have both maker and taker payment account payloads available.
* If payment account payloads are incomplete, no response is sent.
* The response contains the taker account payload and maker account payload.
* The response is sent confidentially to the mediator.

---

### Closing mediation

The mediator closes mediation by sending a `CLOSED` state change message.

Rules:

* The current dispute state must be `MEDIATION_OPEN` or `MEDIATION_RE_OPENED`.
* The message must contain a mediation result.
* The message must contain a mediation result signature.
* The mediation result contract hash must match the trade contract.
* The mediation result signature must verify against the mediator public key from the contract.
* Messages without a result or signature are ignored.
* Messages with an invalid result signature are ignored.
* If no mediation result is stored yet, the result and signature are stored.
* If a different result or signature is received after one is already stored, the changed result or signature is ignored.
* The dispute state is set to `MEDIATION_CLOSED`.
* The trade is persisted when the dispute state changes.
* The trade channel keeps the mediator as dispute agent.

---

### Accepting or rejecting the mediation result

After a mediation result is stored, each trader can accept or reject it.

Rules:

* A trader can accept or reject only after a mediation result is present.
* The trader's own acceptance value is stored only once.
* A `MuSigMediationResultAcceptanceMessage` is sent confidentially to the peer only when the trader's acceptance value is recorded for the first time.
* A trade log message records whether the trader accepted or rejected the mediation result only when the trader's acceptance value is recorded for the first time.

Rules for receiving peer acceptance:

* The message must reference an existing trade and channel.
* The sender must be the peer from the trade.
* The sender must not be banned.
* Messages from other senders are ignored.
* Messages from banned senders are ignored.
* If no mediation result is stored yet, the message is kept pending.
* The peer acceptance value is stored only once.

---

### Reopened mediation

The mediator can reopen a closed mediation case.

Rules:

* A `RE_OPENED` state change is applied only from `MEDIATION_CLOSED`.
* If the trade is not yet closed, the message is kept pending.
* Duplicate `RE_OPENED` messages are ignored.
* The dispute state is set to `MEDIATION_RE_OPENED`.
* The trade is persisted when the dispute state changes.
* The trade channel is updated to use the mediator as dispute agent.

---

### Invariants

* A mediation request belongs to exactly one `tradeId`.
* A trader can request mediation only once from `NO_DISPUTE`.
* Only the contract mediator can send mediation state changes.
* The mediator must not be banned.
* The non-requesting peer sends dispute case data after mediation opens.
* Payment details are sent only after an explicit mediator request.
* A stored mediation result cannot be changed.
* A stored mediation result signature cannot be changed.
* A mediation result must verify against the mediator public key from the contract.
* Peer acceptance is stored only after a mediation result exists.
* A trader's own acceptance and peer acceptance are each recorded only once.
* Mediation state changes do not override arbitration state.
