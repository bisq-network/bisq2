# MuSig mediation specifications

Status: **Java happy-path custom-payout finalization is implemented; service-side broadcast and recovery remain outstanding.**

The mediated custom payout integration is tracked by
[bisq-network/bisq2#4888](https://github.com/bisq-network/bisq2/issues/4888). The custom payout RPC definitions are
already present, but the complete Java flow and the required MuSig service behavior are not.

The current Java branch implements result acceptance, local PSBT creation, result-bound PSBT and rejection messages,
party-level persistence, and FSM integration. It also provides a guarded `CustomCloseTrade` handler and a transition
from `CUSTOM_PAYOUT_SIGNED` to `CUSTOM_PAYOUT_CLOSED_TRADE`. After either local signing or peer-PSBT processing, Java
emits the internal finalization event when both partial PSBTs exist and their claimed transaction IDs match.

The current implementation also temporarily omits the returned-payout upper-bound checks because trade setup still
passes fixed 30,000-sat security deposits to the MuSig service instead of the contract's collateral amounts. This
prerequisite must be corrected before the checks are restored and the settlement is safe to finalize.

Custom signing is gated on the `DEPOSIT_TX_CONFIRMED` trade state, but automatic confirmation observation is disabled in
the current development implementation. The state is currently reached through the existing development skip action.

The specification is split by concern:

* [Mediated custom payout settlement](custom-payout-settlement.md) defines the domain flow, prerequisites, state
  boundaries, payout rules, and first-version failure behavior.
* [Mediated custom payout interface](custom-payout-interface.md) defines the Java, MuSig service, and P2P responsibilities
  and the contract at their boundaries.

The package-local
[trader mediation specification](../../../../../trade/src/main/java/bisq/trade/mu_sig/mediation/specification.md)
describes the wider Java trader-mediation implementation. It remains separate because these documents focus on the
custom-payout settlement and its Java/MuSig service boundary.
