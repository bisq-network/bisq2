# Mediated custom payout interface for MuSig trades

## Status and purpose

This document defines the proposed contract between Bisq2 Java, the local MuSig service, and the traders' P2P protocol
for the first mediated custom payout integration. It complements the
[settlement specification](custom-payout-settlement.md).

The existing `SignCustomPayoutTx` and `CustomCloseTrade` RPC schemas remain unchanged. This document does not assume that
all required service behavior is implemented; in particular, broadcast is currently a placeholder.

## Responsibility boundary

| Concern | Owner | Required behavior |
|---|---|---|
| Mediation eligibility and trader decision | Java | Validate the signed result and decide whether local signing is permitted. |
| Application state and persistence | Java | Retain local and peer settlement artifacts and coordinate the normal trade state. |
| Trader-to-trader communication | Java/P2P | Exchange authenticated, result-bound partial PSBTs and result-bound rejections. |
| Custom payout RPCs | MuSig service | Return the local partial PSBT and report successful custom closure only with the interface guarantees below. |
| Local trade completion | Java | Complete after a successful `CustomCloseTrade` response in the first version. |

Java does not construct or sign the Bitcoin transaction. The mediator signs the proposed distribution but does not sign
the Bitcoin transaction.

## gRPC contract

### `SignCustomPayoutTx`

Java calls `SignCustomPayoutTx(CustomPayoutPsbtRequest)` to request a locally signed partial PSBT.

`CustomPayoutPsbtRequest` contains:

| Field | Meaning |
|---|---|
| `tradeId` | Selects the existing service-side trade context. |
| `sellersPayoutAmountExcludingFee` | The exact seller gross payout from the immutable signed mediation result, before custom payout fees. Both roles pass the same value. |
| `feeRate` | Fee rate in satoshis per 1,000 weight units (`sat/kwu`). |

The request does not contain the buyer gross payout amount.

On success the service returns `CustomPayoutPsbt`:

| Field | Meaning |
|---|---|
| `psbt` | The custom payout PSBT partially signed by the local trader for both inputs. |
| `txId` | The transaction ID of the common unsigned custom payout transaction. |
| `buyersPayoutAmountIncludingFee` | The actual buyer output value after the buyer fee share was deducted. |
| `sellersPayoutAmountIncludingFee` | The actual seller output value after the seller fee share was deducted. |

The two payout field names are potentially misleading: they are post-fee output values, not gross amounts with an
additional fee. Java validates them against the gross mediation amounts and must not deduct a second fee. Java does not
reproduce service-side fee or dust calculations.

### `CustomCloseTrade`

Java calls `CustomCloseTrade(CustomCloseTradeRequest)` only after it has both its own partial PSBT and the peer's matching,
result-bound partial PSBT.

`CustomCloseTradeRequest` contains:

| Field | Meaning |
|---|---|
| `tradeId` | Selects the service-side trade context and its local partial PSBT. |
| `peersCustomPayoutPsbt` | The peer's serialized partial PSBT received through the P2P protocol. |

On success, `CustomCloseTradeResponse.customPayoutTx` contains the finalized transaction and establishes that the peer
PSBT was accepted for the expected custom payout. It also means the Bitcoin backend accepted that transaction for
broadcast or reported that the identical transaction was already known. It does not mean mempool observation or
blockchain confirmation. The already-known case is important because both clients independently call this RPC for the
same transaction.

### RPC identity

The first integration adds no `settlementId`, mediation-result hash, buyer gross payout, or other RPC field.

At the RPC boundary, `tradeId` selects the service-side trade context and the existing request fields describe the
operation. At the Java/P2P boundary, `mediationResultHash` binds peer artifacts to the signed proposal, while the RPC
response's `txId` identifies the concrete unsigned transaction.

This is sufficient only for the one-shot first iteration. No idempotency or restart guarantee may be inferred from
`tradeId` alone.

## P2P contract

### Positive response: partial PSBT

There is no separate positive-acceptance message. `MuSigCustomPayoutPsbtMessage` is the only positive response sent to
the peer and is carried as a dedicated confidential mailbox message under `MuSigTradeMessage`.

The target flow therefore removes the existing `MuSigMediationResultAcceptanceMessage` and does not persist or consult
the existing `mediationResultAccepted` value.

It uses the normal `TradeMessage` envelope:

* `id`: one stable outgoing ID for this partial PSBT
* `tradeId`
* `protocolVersion`
* `sender`
* `receiver`

Its message-specific payload is:

| Field | Meaning |
|---|---|
| `mediationResultHash` | The 20-byte `DigestUtil.hash(mediationResult.serializeForHash())` of the exact result accepted by the signer. |
| `txId` | The transaction ID returned by the local MuSig service. |
| `psbt` | The serialized partial PSBT returned by the local MuSig service. |

The P2P payload must remain independent of the gRPC DTO. The sender copies the required values from its stored local RPC
result; the receiver represents them as peer/domain data rather than storing a peer-supplied gRPC object.

Each trader sends this message after successful local signing so that either client can later finalize independently.

### Negative response: rejection

`MuSigMediationResultRejectionMessage` communicates rejection of a mediation result whose payout distribution type is
not `NO_PAYOUT`. It identifies the trade and sender and carries `mediationResultHash` so the receiver can verify that both
traders refer to the same immutable result.

Positive intent is never inferred from message delivery or from a Boolean. It is represented by the result-bound local or
peer PSBT. Rejection remains a one-way result-bound fact.

### No final publication message in the first version

The first version has no separate P2P message announcing final transaction publication. Both clients receive the peer
PSBT and independently obtain their own `CustomCloseTradeResponse`. A later publication message may be considered for
recovery, but it cannot replace a successful custom-close response or authoritative chain observation.

### Transport acknowledgements

A transport acknowledgement proves only that the peer's network layer received and decrypted a structurally valid
envelope. It does not prove:

* business or contextual validity
* peer signature validity
* durable peer storage
* readiness to finalize

Transport delivery status must never advance custom payout settlement state by itself.

## Java validation contract

### Before `SignCustomPayoutTx`

Java must verify:

* the local trade and corresponding service-side trade context are expected to exist
* the signed mediation result is immutable, authentic, and bound to the trade contract
* the result's payout distribution type is not `NO_PAYOUT` and the gross payouts distribute the full trade pot
* the exact fully signed DepositTx reached the normal `DEPOSIT_TX_CONFIRMED` transition
* the local state is in the exact signing allowlist defined by the settlement specification
* the peer has not already rejected this result
* local rejection, custom signing, and the message F boundary are serialized per trade
* the provisional fee rate is valid for the first iteration

This check is not an authoritative query of the Bitcoin UTXO set and does not prove that the inputs remain unspent.

### On a local signing response

Java must verify:

* `txId` is present and syntactically valid
* the partial PSBT is present
* both returned actual payouts are non-negative and no greater than their gross payout amounts
* the response does not conflict with an already stored response for this trade

Java stores the successful response before constructing the P2P message.

The mediation result and its mediator signature are write-once for the lifetime of the trade. Reopening mediation through
`MEDIATION_RE_OPENED` changes the dispute state but does not permit replacing either value. The local signing
response is therefore associated with the trade's single stored result. Before constructing the outgoing P2P message or
invoking `CustomCloseTrade`, Java requires the stored result and its verified signature to be present and computes
`mediationResultHash` from that result. It does not persist a duplicate result hash with the local RPC response.

### On a peer PSBT

Java validates in this order:

1. Authenticate and validate the normal message envelope, trade ID, protocol version, sender, and receiver. The sender
   must be the expected trade peer and must not be banned.
2. Require a syntactically valid 64-character hexadecimal transaction ID.
3. Require a non-empty PSBT within a named implementation size limit.
4. If the immutable mediation result is not yet available, keep the exact message pending in the existing live-process
   MuSig pending-message mechanism and do not use it.
5. Recompute the hash of the stored result and require it to equal `mediationResultHash`.
6. Reject the PSBT if a peer rejection for this result was stored first.
7. Once the local PSBT exists, require the peer's claimed `txId` to equal the locally returned `txId`.
8. Store peer data before invoking `CustomCloseTrade` and never replace it with conflicting data.

The two serialized PSBTs are not expected to be equal. The peer's claimed transaction ID is a Java pre-check only;
`CustomCloseTrade` success is required before Java treats the peer PSBT as accepted for the expected transaction.

### Before local completion

Java calls `CustomCloseTrade` only when the immutable result, local PSBT, and contextually valid matching peer PSBT are
available. On success, Java validates and stores the returned final transaction before moving to the existing buyer or
seller closed state.

## Persistence and ordering contract

Custom payout progress is related to, but separate from, `MuSigTradeState` and `MuSigDisputeState`. The implementation
must inspect these dimensions together rather than create a combined enum value for every possible combination.

Each `MuSigTradeParty` owns optional grouped `MuSigCustomPayoutPartyData`. The persisted model distinguishes data produced
locally from data received from the peer:

* local party data retains the successful local `CustomPayoutPsbt` response and may later retain the successful
  `CustomCloseTradeResponse`
* peer party data retains an independent `PeerCustomPayoutPsbt` containing the claimed transaction ID and serialized peer
  PSBT without embedding a gRPC DTO
* rejection remains a one-way fact associated with the rejecting local or peer party

The normal `TradeMessage.id` and the message payload's `mediationResultHash` are validated at the P2P boundary but are not
duplicated in party persistence. The existing network resend and delivery infrastructure owns the complete outgoing
message and its stable ID.

Fields are write-once. Repeating the same stored value is a no-op, and conflicting peer data must not replace the first
stored value. Peer rejection and a peer PSBT are mutually exclusive decision artifacts, and their check-and-store
operations must be serialized per trade.

Payout amounts, the signed mediation result, normal state, dispute state, DepositTx confirmation, network delivery status,
and the provisional fee rate remain owned by their existing models and must not be duplicated in custom payout data. Both
local and peer custom payout data rely on the immutable mediation result owned by the trade dispute model; Java recomputes
`mediationResultHash` from that result when needed.

The first version uses the normal MuSig handler and FSM persistence flow after successful processing. It adds no special
pre-call requested state, awaited peer-message checkpoint, or durable pending-message queue.

## Error and completion contract

For the first iteration:

* `SignCustomPayoutTx` and `CustomCloseTrade` are one-shot calls with no automatic retry.
* A signing error or missing response produces no outgoing PSBT and keeps the normal close path blocked after dispatch.
* A close error or missing response does not select another settlement or reopen normal cooperative closure.
* Peer unavailability causes an indefinite pending state; a rejection received before a PSBT causes a blocked state.
* A successful `CustomCloseTrade` response completes the local Java trade without waiting for confirmation.

These rules are conservative because an RPC transport failure does not reveal whether the service changed state, signed,
or accepted a transaction for broadcast before the response was lost.

## Joint contract still to confirm

The following server-side interface behavior must be agreed before production recovery behavior is built:

* authoritative checking of whether the expected inputs have already been spent
* durable storage of the service-side trade context, local PSBT, final transaction, and broadcast outcome
* idempotent replay and restart reconciliation under the unchanged RPC schemas
* structured errors that distinguish permanent, retryable, and ambiguous outcomes
* reconciliation after a lost `SignCustomPayoutTx` or `CustomCloseTrade` response
* role-specific fallback after a custom payout signature has been released
* transaction-status reporting, conflicting-spend detection, and reorganization behavior

These open points do not change the first-version happy path, but they prevent treating that first iteration as a complete
production recovery design.
