# Mediated custom payout interface for MuSig trades

## Status and purpose

This document records the current contract between Bisq2 Java, the local MuSig service, and the traders' P2P protocol
for the first mediated custom payout integration, together with the remaining broadcast and recovery requirements.
It complements the
[settlement specification](custom-payout-settlement.md).

The existing `SignCustomPayoutTx` and `CustomCloseTrade` RPC schemas remain unchanged. Java currently integrates
`SignCustomPayoutTx`, persists and exchanges partial PSBTs, and communicates rejection. A guarded handler and FSM
transition for `CustomCloseTrade` are implemented. The internal finalization event is emitted after either local signing
or peer-PSBT processing once both matching partial PSBTs are available. Service-side broadcast is currently a
placeholder.

## Responsibility boundary

| Concern | Owner | Required behavior |
|---|---|---|
| Mediation eligibility and trader decision | Java | Validate the signed result and decide whether local signing is permitted. |
| Application state and persistence | Java | Retain local and peer settlement artifacts and coordinate the normal trade state. |
| Trader-to-trader communication | Java/P2P | Exchange context-validated, result-bound partial PSBTs and result-bound rejections. |
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
additional fee. Java is intended to validate them against the gross mediation amounts and must not deduct a second fee.
The current validation exception is documented below. Java does not reproduce service-side fee or dust calculations.

### `CustomCloseTrade`

The Java request and response value objects, event handler, FSM transition, and readiness trigger are implemented. The
handler calls `CustomCloseTrade(CustomCloseTradeRequest)` only after requiring both the local partial PSBT and the peer's
matching, result-bound partial PSBT.

`CustomCloseTradeRequest` contains:

| Field | Meaning |
|---|---|
| `tradeId` | Selects the service-side trade context and its local partial PSBT. |
| `peersCustomPayoutPsbt` | The peer's serialized partial PSBT received through the P2P protocol. |

Under the completion contract, a successful `CustomCloseTradeResponse.customPayoutTx` contains the finalized
transaction and establishes that the peer PSBT was accepted for the expected custom payout. It also means the Bitcoin
backend accepted that transaction for broadcast or reported that the identical transaction was already known. It does
not mean mempool observation or
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

The current flow removes `MuSigMediationResultAcceptanceMessage` and does not persist or consult a
`mediationResultAccepted` value.

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

Each trader sends this message after successful local signing. Each client independently calls `CustomCloseTrade` once
both matching partial PSBTs are available and the finalization prerequisites are satisfied.

### Negative response: rejection

`MuSigMediationResultRejectionMessage` communicates rejection of a mediation result whose payout distribution type is
not `NO_PAYOUT`. It identifies the trade and sender and carries `mediationResultHash` so the receiver can verify that both
traders refer to the same immutable result.

Positive intent is never inferred from message delivery or from a Boolean. It is represented by the result-bound local or
peer PSBT. Rejection remains a one-way result-bound fact.

### No final publication message in the current implementation

There is no P2P message announcing final transaction publication. Each client independently obtains its own
`CustomCloseTradeResponse` and completes its local trade after a successful response containing a non-empty transaction,
without waiting for blockchain confirmation. A later publication message may be considered for recovery, but it cannot
replace a successful custom-close response or authoritative chain observation.

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

* the local trade protocol exists; the service-side context is assumed to exist and is selected by `tradeId`
* the signed mediation result is immutable, authentic, and bound to the trade contract
* the result's payout distribution type is not `NO_PAYOUT` and the gross payouts distribute the full trade pot
* the trade reached the normal `DEPOSIT_TX_CONFIRMED` transition
* the local state is in the exact signing allowlist defined by the settlement specification
* the peer has not already rejected this result
* local rejection, custom signing, and normal-close handlers are serialized on the same trade protocol monitor
* the provisional fee rate is valid for the first iteration

This check is not an authoritative query of the Bitcoin UTXO set and does not prove that the inputs remain unspent.

Desktop button availability uses lightweight checks on UI-owned values supplied by the trade and dispute observers:
the deposit-confirmed signing window, closed mediation with a result other than `NO_PAYOUT`, no local decision or peer
rejection. These checks neither acquire a protocol lock nor schedule an eligibility
query. There is no service-side availability cache. Signature and payout validation remain in the service; an enabled
button does not guarantee acceptance, and failed validation must prevent the signing RPC.
Acceptance, rejection, and mediation/arbitration requests are fire-and-forget commands executed on the MuSig trade
executor under the trade protocol monitor. The desktop observes trade, dispute, rejection, and error state; these
commands return no future and have no separate decision-in-progress flag. A request that is no longer eligible is
ignored without a UI completion callback. Repeated clicks are rechecked when processed and must not create another
signature. Rejection and a requested arbitration escalation execute in one task, with arbitration requested only if
the local rejection is recorded. Changing the selected trade discards obsolete observer callbacks but does not cancel
an already submitted trader decision. A pending RPC holds only its trade's protocol monitor, although work can still
wait for executor capacity.

### On a local signing response

The current implementation verifies:

* `txId` is present and syntactically valid
* the partial PSBT is present, non-empty, and no larger than 4,096 bytes
* both returned actual payouts are non-negative
* no local custom-payout response was already stored, as enforced by the signing gate

The intended checks that each actual payout is no greater than its corresponding gross mediation payout are temporarily
disabled. Trade setup still sends fixed 30,000-sat security deposits to the MuSig service while the mediation result uses
the contract's collateral percentages, so the two payout pools can differ. This is a known prerequisite bug, not a reason
to treat a larger returned payout as valid. The setup amounts must be aligned and these checks restored before custom
payout finalization is safe for production.

Java stores the successful response before constructing the P2P message.

The mediation result and its mediator signature are write-once for the lifetime of the trade. Reopening mediation through
`MEDIATION_RE_OPENED` changes the dispute state but does not permit replacing either value. The local signing
response is therefore associated with the trade's single stored result. Before local signing and construction of the
outgoing P2P message, Java validates the stored result and mediator signature and computes `mediationResultHash` from
that result. The finalization handler requires the same result and signature to remain present. Java does not persist a
duplicate result hash with either local RPC response.

### On a peer PSBT

Java validates in this order:

1. Structurally validate the message when it is constructed, reject a banned sender in `MuSigTradeService`, and require
   the expected trade ID, protocol version, sender, and receiver in the contextual handler.
2. Require a syntactically valid 64-character hexadecimal transaction ID.
3. Require a non-empty PSBT within a named implementation size limit.
4. If the immutable mediation result is not yet available, keep the exact message pending in the existing live-process
   MuSig pending-message mechanism and do not use it.
5. Recompute the hash of the stored result and require it to equal `mediationResultHash`.
6. Reject the PSBT if a peer rejection for this result was stored first.
7. Once the local PSBT exists, require an incoming peer `txId` to equal the locally returned `txId`.
8. Store peer data and never replace it with conflicting data.

The two serialized PSBTs are not expected to be equal. The peer's claimed transaction ID is a Java pre-check only;
the peer-message handler does not itself pass the peer PSBT to `CustomCloseTrade` for authoritative validation. If the
peer PSBT was stored first and local signing later returns a different `txId`, the current handler logs the mismatch but
still stores and sends the local result. This does not establish that either transaction ID is correct. The completion
handler requires equality before calling `CustomCloseTrade`, where the service must validate the actual peer PSBT.

### Before local completion

The finalization handler requires the immutable result, its signature, both partial PSBTs, and matching claimed
transaction IDs. It then calls `CustomCloseTrade`, rejects an empty final transaction, persists the response, and enters
`CUSTOM_PAYOUT_CLOSED_TRADE` without waiting for blockchain confirmation.

After processing either local acceptance or a mediation-settlement message, Java evaluates finalization readiness under
the trade protocol monitor. The event is emitted only from `CUSTOM_PAYOUT_SIGNED`, with mediation still closed, neither party
rejected, both partial PSBTs present, no stored close response, and matching claimed transaction IDs.

## Persistence and ordering contract

Custom payout progress is related to, but separate from, `MuSigTradeState` and `MuSigDisputeState`. The implementation
inspects these dimensions together rather than creating a combined enum value for every possible combination. Successful
local signing is nevertheless represented by the dedicated non-final `CUSTOM_PAYOUT_SIGNED` trade state.

Each `MuSigTradeParty` owns optional grouped `MuSigCustomPayoutPartyData`. The persisted model distinguishes data produced
locally from data received from the peer:

* local party data retains the successful local `CustomPayoutPsbt` response and, after finalization, the successful
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

The current version uses the normal MuSig handler and FSM persistence flow after successful processing. It adds no
special pre-call requested state, awaited peer-message checkpoint, or durable pending-message queue. Settlement messages
that lack a trade protocol or signed-result context are held only in an in-memory per-trade queue. If the signed result is
available but the FSM state is too early, the existing non-persisted FSM event queue holds the message until a later state
transition. Those queues themselves provide no restart recovery; any later replay depends on the separate network
delivery layer.

## Error and completion contract

Current Java behavior is:

* Each handled acceptance event makes one blocking `SignCustomPayoutTx` call, with no automatic retry or
  requested/unknown checkpoint.
* A thrown signing error produces no stored or outgoing PSBT; normal FSM error handling moves the trade to `FAILED`.
* If the blocking RPC never returns, its asynchronous task remains blocked and no state transition occurs.
* Peer unavailability leaves custom settlement pending indefinitely; a rejection received before a peer PSBT blocks
  custom settlement but does not itself stop the normal trade FSM.
* The configured finalization handler makes one blocking `CustomCloseTrade` call. A thrown error follows normal FSM
  error handling and moves the trade to `FAILED`; no automatic retry or ambiguous-outcome recovery exists.
* Receiving both matching partial PSBTs emits the finalization event and completes the local Java trade after a
  successful response.

An RPC transport failure does not reveal whether the service changed state or signed before the response was lost.
Because starting the request is not persisted, a process restart before the response can permit another acceptance
attempt. The current Java flow does not reconcile that ambiguity.

## Known Java implementation gaps

The current integration still has these production gaps:

* trade initialization must derive buyer and seller security deposits from the contract's `CollateralOption` instead of
  passing fixed 30,000-sat values to the MuSig service
* the local signing-response checks against both gross mediation payouts must be restored
* automatic DepositTx confirmation observation must be enabled or replaced; the current development code returns before
  subscribing and relies on the development skip action to enter `DEPOSIT_TX_CONFIRMED`
* restart and ambiguous-RPC-outcome behavior remains undefined

## Joint contract still to confirm

The following server-side interface behavior must be agreed before production recovery behavior is built:

* authoritative checking of whether the expected inputs have already been spent
* durable storage of the service-side trade context, local PSBT, final transaction, and broadcast outcome
* idempotent replay and restart reconciliation under the unchanged RPC schemas
* structured errors that distinguish permanent, retryable, and ambiguous outcomes
* reconciliation after a lost `SignCustomPayoutTx` or `CustomCloseTrade` response
* role-specific fallback after a custom payout signature has been released
* transaction-status reporting, conflicting-spend detection, and reorganization behavior

These open points prevent treating the implemented signing, PSBT exchange, and finalization flow as a complete production
recovery design.
