## Specification for the take offer process

### Offer as root input

The taken `MuSigOffer` is the root input of the take-offer process. It fixes:

* the market
* the `offerDirection`
* the price specification (fixed price, floating price with its percentage, or market
  price; the create-offer specification currently lists only fixed and floating —
  `MarketPriceSpec` exists in the domain and must be handled here regardless, so the
  create spec needs a corresponding update)
* the amount specification (fixed amount or amount range)
* the payment method specifications for both sides: the Bitcoin side carries a single
  Bitcoin main-chain specification (created by the offer itself), the other side carries
  the maker's selected payment methods
* the offer options. A MuSig offer produced by the current create-offer process
  contains exactly two kinds: one `AccountOption` per maker payment method
  (auto-derived from the maker's account: salted account id, country code, accepted
  country codes, bank id, accepted banks, salted account payload hash) and one
  `CollateralOption` defining the buyer and seller security deposit percentages
  (currently static 25%/25%, not maker-editable). The take wizard reads both kinds:
  the `CollateralOption` (security deposit display on the review step) and the
  `AccountOption` compatibility data (taker account eligibility filtering, see Payment
  method).
  `AccountOption` is additionally consumed at handoff: contract creation embeds the
  maker's salted account payload hash for the selected payment method from it — one
  reason the shape validation below requires one `AccountOption` per selectable
  method. The domain model also carries option kinds the take process does not
  consume (e.g. `FeeOption`); those are ignored. Offers containing option kinds unknown to this
  client fail at protobuf deserialization before any take flow exists — a wire-level
  constraint outside this specification. The shape rules in the validation list below
  apply to the known kinds.

The offer originates from the P2P network and is untrusted input. At initialization the
use case validates it at the trust boundary (per `docs/dev/input_validation_policy.md`):

* the price specification must be resolvable against the selected market (a missing
  market price prevents the take process from starting, see Price)
* a floating price specification must carry a percentage within the create-offer bounds
  (−10% to +50%); an offer outside them could not have been created legitimately and is
  rejected
* a fixed price specification must carry a price quote whose market equals the offer's
  market
* the resolved price quote must lie within the maximum deviation from the current
  market price, once that bound is defined (see Price)
* the taker-selectable side (the non-Bitcoin side) must contain between one and four
  payment method specifications, matching the create-offer bounds
* the offer options must contain exactly one `CollateralOption`, with equal buyer and
  seller security deposit percentages within sane bounds (0–100%) — asymmetric deposits
  are not supported by the current protocol and are rejected — and exactly one `AccountOption`
  per taker-selectable payment method (duplicates for a method are rejected — contract
  creation must resolve the maker's account hash unambiguously); offers violating this
  shape are rejected
* the **Bitcoin side** must contain exactly one `BitcoinPaymentMethodSpec` whose payment
  rail is `BitcoinPaymentRail.MAIN_CHAIN`. MuSig settles the Bitcoin leg through its own
  on-chain transactions, so no other Bitcoin settlement rail (e.g. Lightning) is valid
  for this protocol.

In addition to the trust-boundary validation, the take process checks preconditions at
initialization:

* the offer must not have been created by any of the user's local user profile
  identities (own offers are rejected regardless of the currently selected profile)
* the offer must list the MuSig protocol among its supported protocol types, otherwise
  it is rejected
* if the user has already taken this offer before, taking is still allowed but requires
  an extra confirmation (mirrors Bisq Easy's already-taken dialog; each take creates a
  distinct trade since the trade id includes the take date). "Already taken" means any
  currently persisted trade of this user for this offer id, regardless of trade state —
  open and failed attempts both count; a trade the user has closed is removed from the
  trade store and deliberately no longer triggers the confirmation

The offer's amounts are deliberately not validated against the absolute limits at
initialization. The absolute limits are USD-defined and converted at the current market
price, so an honestly created offer can drift outside them through price movement alone.
Instead, the amount limits domain intersects the offer range with the current limits
(see Amount limits), which narrows the takeable range gracefully instead of rejecting
the offer at the door.

If a validation fails, the take process is rejected at initialization: no wizard step is
shown, an error is surfaced to the user, and no partially initialized take state
remains. The Bitcoin side check guards against malformed or malicious offers received
over the network; offers created by the current code satisfy it by construction.

Unlike the create-offer process, market, direction, and price are **not selectable**.
They are derived once at initialization and do not change for the lifetime of the take
process — for price this means the price *specification* is fixed; the *resolved quote*
still follows market price updates (see Price). The only user selections are the
**payment method/account** and, for range offers, the **trade amount**.

Status note: Altcoin-Bitcoin markets are specified throughout this document for
congruence with the create-offer specification and are supported by the domain model,
but they are not reachable end to end today. Prerequisites before altcoin offers can be
taken: crypto-asset account creation is not production-complete (per-asset validation
and some account types are unfinished), and the altcoin market must have an available
market price for the USD based limit conversions. Until then, Bitcoin-Fiat markets are
the operative scope.

---

### Direction

The taker direction is the mirror of the maker offer direction. For the taker,
`displayDirection` is the taker's own direction as shown in the take-offer UI, i.e. the
mirror of the maker's `displayDirection`.

* A taker who takes a **SELL** offer in a Bitcoin-Fiat market buys Bitcoin
  (`displayDirection` **BUY**).
* For Altcoin-Bitcoin markets, the same mirroring applies to the Bitcoin-side
  `offerDirection` as defined in the create-offer specification.

The direction determines whether the taker is the **Bitcoin buyer**. The
**user-specific trade amount limit** applies only when the taker is the Bitcoin buyer in
a Bitcoin-Fiat market (i.e. the taken offer has `offerDirection` **SELL**).

#### Dependencies / update triggers

* Direction is derived from the offer at initialization and never changes afterwards.
* No runtime update triggers.

---

### Market

The market is fixed by the offer.

The market determines:

* which side of the offer's payment methods the taker selects from
* account eligibility (accounts must support the market's relevant currency code)
* the conversion rules between USD-defined limits and trade amounts

#### Dependencies / update triggers

* Market is derived from the offer at initialization and never changes afterwards.
* No runtime update triggers.

---

### Payment method

The taker selects exactly **one** payment method out of the methods offered by the maker:

* In **Bitcoin-Fiat markets** the taker selects from the offer's **quote side** (fiat)
  payment method specifications. The base side must contain exactly one specification.
* In **Altcoin-Bitcoin markets** the taker selects from the offer's **base side**
  payment method specifications.

Account rules:

* Only accounts matching the required currency code are eligible. User defined
  (free-form) fiat accounts are not eligible.
* Accounts must also satisfy the compatibility data of the offer's `AccountOption` for
  the method (accepted country codes, accepted banks): an account from a country the
  maker does not accept, or held at a bank the maker does not accept, is not eligible.
  A dimension applies only when the offer's `AccountOption` carries entries for it: an
  empty accepted-countries or accepted-banks list imposes no restriction for that
  dimension (non-country and non-bank payment methods store empty lists by
  construction). Bisq 1 applies the same kind of filtering (offers restricted to
  specific banks, for example for lower transfer fees).
* If no eligible account exists for a chosen payment method, a popup prompts the user
  to create one. The method stays clickable — choosing it triggers the account-creation
  prompt — but the take cannot proceed with it until an eligible account exists. This
  applies in every path, including a single-method offer where the taker has no
  account yet. The disabled-with-reason rule below stays amount-range-scoped: methods
  failing the amount rule are disabled, methods lacking only an eligible account are
  not.
* If multiple eligible accounts exist for the selected method, a dropdown is shown.
* If exactly one eligible account exists, it is selected automatically.
* If exactly one eligible account exists in total across the offered methods, its
  payment method and account are preselected (as in the create-offer specification; the
  payment step is still shown unless the bypass below applies).

Step bypass:

* If both side specification lists of the offer contain exactly one payment method,
  the taker-side method is determined (quote side in Bitcoin-Fiat markets, base side in
  Altcoin-Bitcoin markets). If exactly one eligible account exists for that method, the
  payment step is skipped and the method and account are applied automatically. In all
  other cases the payment step is shown.

Payment method selection affects the **payment-method-specific amount limit**: for range
offers, the effective maximum is bounded by the rail limit of the **selected** method
(not by the strictest method of the offer, which already bounded the maker's range).

A payment method is selectable only if choosing it yields a non-empty effective amount
range (for fixed amount offers: only if it admits the fixed amount). Methods failing
this are shown disabled with the reason. Only if no offered method qualifies is the
offer untakeable (see Amount limits).

The selection is constrained to the offer's payment method specifications. Downstream,
contract creation accepts the spec as passed and the peer verification only compares
the two parties' contracts for equality — neither checks membership against the offer —
so the use case is the enforcement point for the membership guarantee stated in
Handoff.

#### Dependencies / update triggers

* The selectable methods and eligible accounts are derived from the offer and the
  account service at initialization.
* Changing the selected payment method or account requires updating the
  payment-method-specific amount limit.
* When the payment-method-specific amount limit changes, the effective amount limits and
  the clamped trade amount must be recomputed.

---

### Price

The price is fixed by the offer and not selectable.

The offer's price specification is resolved to a **price quote** at initialization:

* **Fixed price**: used as stored in the offer.
* **Floating price**: current market price adjusted by the stored percentage.
* **Market price**: current market price.

A current market price is required for every take process, regardless of the price
specification kind: fixed price offers need no market price for quote *resolution*, but
the USD-defined limit conversions and the trade handoff require one. If no market price
is available, the take process cannot proceed and an error state is surfaced instead of
a partially initialized flow.

The resolved price quote is used to calculate the **passive amount** from the active
amount and to convert USD-defined limits.

Fixed price offers have no create-side bound, so a fixed price can sit arbitrarily far
from the current market price. The take process applies the following rules:

* The take process shows a **deviation warning** when the resolved price quote deviates
  from the current market price by more than a configurable threshold — a user setting
  with a 10% default. The mechanism mirrors Bisq 1's deviation warning (Bisq 1 defaults
  to 15%); the 10% default is the product choice here. The setting is distinct from the
  existing max-trade-price-deviation setting (Bisq Easy's peer-price tolerance, 5%
  default) — implementations must not reuse that knob. The warning is keyed on the
  resolved quote, so it applies regardless of price-specification kind; in practice
  floating offers are bounded at creation (−10% to +50%) but can still exceed the
  threshold. The warning informs; it does not block the take.
* Beyond a **maximum deviation** (bound TBD; a fixed rule, not a user setting), such
  offers are treated as invalid in the offerbook and cannot be taken; makers running
  extreme fixed prices accept that their offers become invalid when the distance to
  the market price grows too large. Whether invalid offers are hidden or shown as
  invalid is an offerbook-domain decision, outside the take-offer use case (note:
  distinct from the maker-side deactivate-offer action in the current codebase).
  Because the market price is client-local and the offerbook cannot be assumed to have
  filtered, the take process rejects an offer beyond the maximum deviation at
  initialization; if the market price crosses the bound while a take is open,
  confirmation is blocked with a reason and lifts on recovery, following the
  background-change rules in Amount limits. The bound's value is an open product
  decision, to be settled with the offerbook-domain design; until it is defined, only
  the warning rule applies.
* The maximum bound takes precedence over the warning: an offer beyond it is rejected
  or blocked outright, with no warning dialog; the warning covers deviations between
  the user threshold and the maximum bound.

#### Dependencies / update triggers

* Price resolution depends on the offer's price specification and the current market
  price at initialization.
* If the market price updates while the take process is open, the resolved quote for
  floating and market price offers is refreshed and passive amounts and USD conversions
  are recomputed. The entered (active) amount is treated as the stable value.
* Market price updates re-evaluate the deviation warning and the maximum-deviation
  bound while the process is open.

---

### Amount

For **fixed amount** offers no amount selection exists. The trade amount is the offer
amount; the passive side is derived from the resolved price quote. The amount step is
skipped. If the fixed amount lies outside the effective amount limits (for example above
the taker's user-specific trade amount limit), the offer **cannot be taken**: the take
process is rejected and the reason is surfaced to the user. The amount of a fixed offer
is never clamped, as that would change the maker's offer.

For **range amount** offers the taker selects the trade amount within the effective
amount limits:

* Input via text or slider.
* When the amount step opens, the trade amount is initialized to the midpoint of the
  effective range (rounded on the non-Bitcoin side in Bitcoin-Fiat markets).
* Input can use the **Bitcoin side** or the **non-Bitcoin side**; the passive amount is
  calculated automatically from the resolved price quote.
* Changing the active input side requires recomputing input amount limits and slider
  mapping.

The taker never changes the range bounds; the selection is always a single value inside
the computed effective range. If that computed range collapses to a single value —
either because the intersection with the limits squeezes it to a point, or because
minimum and maximum become indistinguishable at display precision (compared on the
rounded non-Bitcoin side value in Bitcoin-Fiat markets) — there is nothing to select:
the amount is treated as fixed at that value and the amount step is skipped.

#### Dependencies / update triggers

* Amount depends on the offer's amount specification.
* Amount depends on the resolved price quote.
* Amount depends on the effective amount limits.
* Changing the selected payment method affects the amount indirectly through the
  effective amount limits.

---

#### Amount limits

The take-offer amount limits sub-domain consists of:

* `offer amount range` (the base range; per Offer as root input, the offer amounts are
  deliberately not re-validated against the absolute limits at initialization — drift
  is handled by this intersection)
* `absolute amount limits` (the same USD-defined bounds as in the create-offer
  specification)
* `payment-method-specific amount limit` (rail limit of the taker's selected method)
* `user-specific trade amount limit`
* `effective amount limits`

The effective range is the intersection of the offer amount range with the absolute
limits, the payment-method-specific limit and, when applicable, the user-specific
limit.

If the intersection is empty at initialization, the offer **cannot be taken**: the
take process is rejected and the reason is surfaced to the user. If a background
update empties it while the process is open, confirmation is blocked instead (see
below) — the flow is never closed automatically. For fixed amount offers the same
rules apply when the fixed amount lies outside the effective limits (see Amount).

These rules also apply while the take process is open. Changes fall into two classes:

* **User-initiated changes** (switching the payment method or the active input side):
  the selected amount is clamped visibly into the new effective range — the user acted
  and sees the result immediately. This is what the "clamped trade amount"
  recomputation in the Payment method triggers refers to.
* **Background changes** (market price updates): the selected amount is never silently
  clamped. The recomputation re-validates the selected amount and the effective range;
  if they become invalid (empty range, or a fixed amount falling outside the limits),
  confirmation is blocked and the reason is surfaced; the block lifts if a later update
  restores validity. The flow is never closed automatically. The maximum price
  deviation bound is re-validated under the same rules (see Price).

##### Dependencies / update triggers

* the `offer amount range`'s USD and passive-side conversions require recomputation on
  market price updates; the range as stored in the offer's amount specification itself
  never changes.
* `absolute amount limits` require update on market price updates (USD conversion).
* `payment-method-specific amount limit` requires update when the selected payment
  method changes and on market price updates.
* `user-specific trade amount limit` requires update on market price updates
  (direction and market are fixed for a take process).
* `effective amount limits` require update when any of the above change.

The USD conversions use the current market price directly, so these triggers fire for
every price specification kind — including fixed price offers, whose resolved quote
never changes.

The absolute, payment-rail, and user-specific limits are internally defined in **USD**
with the same conversion rules as the create-offer specification. The offer amount range
is denominated as stored in the offer's amount specification and is converted via the
resolved price quote where needed for the intersection.

##### User-specific trade amount limit

Applies when the **taker is the Bitcoin buyer in a Bitcoin-Fiat market** (taken offer
direction **SELL**). Not applied in Altcoin-Bitcoin markets or when the taker sells.

The absence of a seller-side cap is deliberate: MuSig reputation limits protect against
buyer bank chargebacks, unlike Bisq Easy where reputation protects buyers from
malicious sellers. Do not reintroduce a seller-side reputation cap here.

Rules (based on the create-offer specification, with the offer range as the base; the
below-minimum case deliberately diverges from the create side — the maker can adjust
their range, the taker cannot):

* If the user-specific limit lies between the pre-user minimum and the pre-user maximum
  (the bounds of the offer ∩ absolute ∩ payment-method intersection), it becomes the
  effective maximum.
* If above the pre-user maximum, it has no effect.
* If below the pre-user minimum, the intersection is empty and the offer cannot be
  taken (see above) — the limit is never relaxed to meet the minimum.

UI behavior:

* The slider's base range is the intersection of the offer range with the absolute and
  payment-method limits (the pre-user range); hard limits are never displayed as
  reachable.
* A **restricted slider track** indicates the user-specific limit within that base
  range.
* Slider interaction is capped at the user-specific limit.
* Contextual information is shown in the UI and a detailed explanation can be opened in
  an overlay view.

The data source for the user-specific limit is the buyer trade-limit concept discussed
in [discussion #4164](https://github.com/bisq-network/bisq2/discussions/4164)
(trade-rate and amount limits derived from the payment rail, account creation date,
imported Bisq 1 account witness, and reputation score, backed by oracle attestations; a
security deposit boost appears in the concept overview but is not yet part of the
technical rule set). Until that concept is finalized the provider remains a
placeholder, shared with the create-offer side.

##### Reserved: wallet affordability check

Once the real wallet is available, affordability is deliberately not an amount-limits
provider: the amount step never bounds the taker by the spendable balance, so a user
without funds can walk through the whole flow. The check runs at the very end instead:
at confirmation the use case verifies that the spendable balance covers the funds the
taker's side must commit to the deposit transaction (their security deposit, their
share of the fees, and, when the taker is the Bitcoin seller, the trade amount); if it
does not, confirmation is blocked with a popup linking to wallet funding. Not
implemented while the wallet backend is mocked.

---

### Review

The review step summarizes the trade before confirmation:

* trade amounts on both sides (to send / to receive from the taker's perspective)
* the resolved price and its details
* the taker's payment method and account
* the security deposit derived from the offer's collateral option
* the trade-fee status and the mining-fee payer (the Bitcoin seller pays the mining
  fee)

MuSig trades carry a trade fee, but the review does not show a numeric amount until
the review and protocol consume the same authoritative fee policy. The protocol
currently uses a hard-coded placeholder during nonce-shares setup; presenting either
that placeholder or a separate UI estimate as the user-facing fee would be
misleading. The review therefore shows `N/A` for the fee and still identifies the
mining-fee payer. Defining the shared fee schedule, deciding whether it keys off the
maximum or actual trade amount for range offers, and agreeing or persisting the fee
are trade-protocol follow-ups outside this use case.

The confirm action starts the trade. The review step owns the take-offer outcome
handling: send timeout, success state, error and peer-error reporting, and the expected
rejection flows (banned or rate-limited profiles, no mediator, no arbitrator). Their
concrete UX is controller behavior and not specified here, but every synchronous or
asynchronous failure must leave the flow in a state where the taker can retry or close.

---

### Handoff to the trade protocol

The take-offer use case ends when the taker confirms on the review step. At that point
the use case hands the trade domain (`MuSigService.takerCreatesProtocol`) a fully
validated input set:

* the taken offer
* the trade amounts on both sides, consistent with the resolved price quote and inside
  the effective amount limits
* the taker's selected payment method specification — the offer's specification object
  passed on as-is (all fields, e.g. `saltedMakerAccountId`), never reconstructed from
  the payment method; selection picks one of the offer's specification objects, so
  equality with the offer holds by construction
* the taker's selected account, which belongs to the selected payment method and
  satisfies the account eligibility rules (currency support, not user-defined,
  compatible with the offer's `AccountOption` data — see Payment method)
* the offer's price specification, passed through unchanged, plus the market price at
  take time (both stored in the contract); the resolved price quote is use-case-internal
  — it drives the amount and limit calculations but is not handed off and is never
  converted into a different price specification. The market price handed off must be
  the same snapshot against which the amounts were last validated at confirmation;
  fetching a fresh price inside the handoff can store a price inconsistent with the
  contract's own amounts

These guarantees are established at the use-case boundary; the trade protocol and the
musigd backend are outside the scope of the take-offer process and must not need to
re-derive or correct any of these values. The trade fee is not part of the handoff
data. Defining and wiring the shared fee policy across the review and protocol is
outside this use case (see Review).
