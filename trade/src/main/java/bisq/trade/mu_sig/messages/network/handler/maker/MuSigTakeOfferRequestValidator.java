/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.trade.mu_sig.messages.network.handler.maker;

import bisq.common.util.StringUtils;
import bisq.bonded_roles.market_price.MarketPrice;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.application.DevMode;
import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.common.monetary.Coin;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.PriceQuote;
import bisq.account.protocol_type.TradeProtocolType;
import bisq.contract.ContractService;
import bisq.contract.ContractSignatureData;
import bisq.contract.Role;
import bisq.contract.mu_sig.MuSigContract;
import bisq.network.identity.NetworkId;
import bisq.account.payment_method.PaymentMethodSpec;
import bisq.offer.amount.spec.AmountSpecUtil;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.mu_sig.MuSigTradeAmountLimitsPolicy;
import bisq.offer.price.PriceUtil;
import bisq.offer.price.spec.FixPriceSpec;
import bisq.offer.price.spec.FloatPriceSpec;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.offer.price.spec.PriceSpec;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.offer.mu_sig.MyMuSigOffersService;
import bisq.trade.exceptions.TradeProtocolFailure;
import bisq.settings.SettingsService;
import bisq.trade.Trade;
import bisq.trade.exceptions.TradeProtocolException;
import bisq.trade.mu_sig.arbitration.MuSigTraderArbitrationService;
import bisq.trade.mu_sig.mediation.MuSigTraderMediationService;
import bisq.trade.mu_sig.messages.network.SetupTradeMessage_A;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.Optional;

/**
 * Validates an incoming take offer request before any trade is created or persisted.
 * Structural and identity rejections carry the same generic wire reason while the log keeps
 * the specific cause, so a crafted request learns nothing from the failure mode; price
 * deviations and dispute-agent mismatches carry their specific reasons, as those are
 * legitimate conditions an honest taker can act on.
 */
@Slf4j
public final class MuSigTakeOfferRequestValidator {
    // 21,000,000 BTC in satoshis. A Bitcoin-side amount cannot exceed the total supply; the cap
    // also keeps the USD-limit conversion below the range where PriceQuote's long arithmetic
    // wraps, which a taker could otherwise exploit to make an enormous amount read as tiny.
    private static final long MAX_BITCOIN_SUPPLY_SATS = 2_100_000_000_000_000L;

    private MuSigTakeOfferRequestValidator() {
    }

    public static void validateIdentity(ContractService contractService, SetupTradeMessage_A message) {
        MuSigContract contract = message.getContract();
        if (contract == null || contract.getOffer() == null) {
            throw reject("The request carries no contract or no offer");
        }
        NetworkId takerNetworkId = contract.getTaker().getNetworkId();
        if (!message.getSender().equals(takerNetworkId)) {
            throw reject("The authenticated message sender does not match the taker in the contract. senderId="
                    + StringUtils.sanitizeForLog(message.getSender().getId()) + ", contract takerId=" + StringUtils.sanitizeForLog(takerNetworkId.getId()));
        }
        if (!message.getReceiver().equals(contract.getOffer().getMakerNetworkId())) {
            // On a node owning several identities the confidential layer only binds the message
            // to the receiving identity; without this check a request naming maker A in the
            // contract but sent to identity B would build a trade mixing both identities.
            throw reject("The message receiver does not match the offer's maker network id. receiverId="
                    + StringUtils.sanitizeForLog(message.getReceiver().getId()) + ", offer makerId=" + StringUtils.sanitizeForLog(contract.getOffer().getMakerNetworkId().getId()));
        }
        String expectedTradeId = Trade.createId(contract.getOffer().getId(),
                takerNetworkId.getId(),
                contract.getTakeOfferDate());
        if (!message.getTradeId().equals(expectedTradeId)) {
            // The trade id the maker will derive from the contract must be the id the message
            // claims; a mismatch would only be rejected after the trade was persisted, leaving
            // a permanent failed trade per crafted message. The attacker-chosen id is
            // sanitized before logging - control characters would allow forged log entries.
            throw reject("The message trade id does not match the id derived from the contract. messageTradeId="
                    + StringUtils.sanitizeForLog(message.getTradeId()) + ", expected=" + expectedTradeId);
        }
        PublicKey takerPublicKey = takerNetworkId.getPubKey().getPublicKey();
        if (!contractService.arePublicKeysMatching(message.getSenderPublicKey(), takerPublicKey)) {
            throw reject("The message sender public key does not match the taker's public key");
        }
        ContractSignatureData signatureData = message.getContractSignatureData();
        if (signatureData == null || !contractService.arePublicKeysMatching(signatureData, takerPublicKey)) {
            throw reject("The contract signature public key does not match the taker's public key");
        }
        boolean signatureValid;
        try {
            signatureValid = contractService.verifyContractSignature(contract, signatureData);
        } catch (GeneralSecurityException | RuntimeException e) {
            // verifyContractSignature also throws on a hash mismatch between the signature data
            // and the contract; any failure mode of a crafted signature must reject, not crash.
            throw reject("The taker's contract signature could not be verified: " + e.getMessage());
        }
        if (!signatureValid) {
            throw reject("The taker's contract signature does not verify");
        }
    }

    public static void validateTakerProfileKnown(UserProfileService userProfileService, SetupTradeMessage_A message) {
        // The handler resolves the taker's profile with orElseThrow after the trade exists; a
        // sender that never published a profile must be rejected before anything is persisted.
        if (userProfileService.findUserProfile(message.getSender().getId()).isEmpty()) {
            throw reject("The taker's user profile is not known");
        }
    }

    public static void validateOffer(MyMuSigOffersService myMuSigOffersService, SetupTradeMessage_A message) {
        MuSigOffer embeddedOffer = message.getContract().getOffer();
        // Only the activated set establishes takeability: deactivated offers stay retained in
        // the store, and the public offerbook is not authoritative for the maker's own offers.
        // Deliberately NO fallback to existing trades for the same offer - accepting a request
        // because the offer was taken before would let a cached copy of a removed offer be
        // replayed indefinitely.
        Optional<MuSigOffer> myOffer = myMuSigOffersService.findActivatedOffer(embeddedOffer.getId());
        if (myOffer.isEmpty()) {
            throw reject("The offer is not one of the maker's activated offers. offerId="
                    + StringUtils.sanitizeForLog(embeddedOffer.getId()));
        }
        if (!myOffer.get().equals(embeddedOffer)) {
            // The id belongs to a genuine offer but the embedded terms differ: the maker's own
            // retained offer is the authority, not the taker-supplied copy.
            throw reject("The embedded offer does not equal the maker's own offer with that id. offerId="
                    + StringUtils.sanitizeForLog(embeddedOffer.getId()));
        }
        if (!myOffer.get().toProto(true).equals(embeddedOffer.toProto(true))) {
            // Offer.equals (Lombok) ignores fields that are still part of the contract hash -
            // notably the Market currency names, which are excluded from equals but not from the
            // hash. A taker could keep every code and economic field yet alter only a display
            // name; equals passes, but the maker would co-sign a contract committing to the
            // forged name. Comparing the serialize-for-hash form includes those fields while
            // still ignoring the deliberately hash-excluded version fields.
            throw reject("The embedded offer differs from the maker's own offer in a hash-relevant field. offerId="
                    + StringUtils.sanitizeForLog(embeddedOffer.getId()));
        }
        // Contract.verify only validates the date, so the taker-authored protocol-type and
        // party-role discriminators reach the maker unchecked. A contract declaring BISQ_EASY,
        // or one whose taker role is MAKER, would be double-signed and persisted with those
        // wrong values into the dispute-visible record; pin them to the canonical MuSig form.
        MuSigContract contract = message.getContract();
        if (contract.getProtocolType() != TradeProtocolType.MU_SIG) {
            throw reject("The contract's protocol type is not MuSig. protocolType=" + contract.getProtocolType());
        }
        if (contract.getMaker().getRole() != Role.MAKER || contract.getTaker().getRole() != Role.TAKER) {
            throw reject("The contract's party roles are not canonical. makerRole=" + contract.getMaker().getRole()
                    + ", takerRole=" + contract.getTaker().getRole());
        }
    }

    public static void validateEconomics(MarketPriceService marketPriceService,
                                         SettingsService settingsService,
                                         MuSigTraderMediationService mediationService,
                                         MuSigTraderArbitrationService arbitrationService,
                                         SetupTradeMessage_A message) {
        MuSigContract contract = message.getContract();
        MuSigOffer offer = contract.getOffer();
        Market market = offer.getMarket();
        // The take date is taker-chosen and only bounded by a Bisq-1-launch..now+2h range in
        // Contract.verify, so a taker could date a take before the offer existed. That date is part
        // of the trade id and drives the UI's remaining-trade-time, so a back-dated take would show
        // as already expired. It cannot precede the offer it takes.
        if (contract.getTakeOfferDate() < offer.getDate()) {
            throw reject("The take offer date predates the offer. takeOfferDate=" + contract.getTakeOfferDate()
                    + ", offerDate=" + offer.getDate());
        }
        long baseSideAmount = contract.getBaseSideAmount();
        long quoteSideAmount = contract.getQuoteSideAmount();
        // The wire encoding is a signed sint64; every comparison below assumes positive values.
        if (baseSideAmount <= 0 || quoteSideAmount <= 0) {
            throw reject("Non-positive contract amounts. base=" + baseSideAmount + ", quote=" + quoteSideAmount);
        }
        if (!contract.getPriceSpec().equals(offer.getPriceSpec())) {
            // The taker constructs the contract with the offer's own price spec; anything else
            // is taker-authored pricing.
            throw reject("The contract price spec does not equal the offer's price spec");
        }
        if (contract.getTaker().getSaltedAccountPayloadHash().map(hash -> hash.length == 0).orElse(true)) {
            // The commitment is optional on the wire and Party.verify only validates it when
            // present; without this check its absence is discovered only after the maker has
            // signed the deposit transaction.
            throw reject("The contract carries no taker account payload commitment");
        }
        if (!offer.getBaseSidePaymentMethodSpecs().contains(contract.getBaseSidePaymentMethodSpec())
                || !offer.getQuoteSidePaymentMethodSpecs().contains(contract.getQuoteSidePaymentMethodSpec())) {
            throw reject("The contract payment method specs are not part of the offer");
        }
        boolean specIsBaseSide = validateAmountSpecMembership(offer, baseSideAmount, quoteSideAmount);
        validatePriceTolerance(marketPriceService, settingsService, offer, specIsBaseSide, baseSideAmount, quoteSideAmount);
        validateUsdLimits(marketPriceService, contract, market, baseSideAmount, quoteSideAmount);
        validateDisputeAgents(mediationService, arbitrationService, contract, offer);
    }

    // The side the offer's amount spec pins is validated exactly against the spec; returns
    // whether that side is the base side, so the tolerance check knows which side is derived.
    private static boolean validateAmountSpecMembership(MuSigOffer offer, long baseSideAmount, long quoteSideAmount) {
        Optional<Monetary> baseMin = AmountSpecUtil.findBaseSideMinOrFixedAmountFromSpec(
                offer.getAmountSpec(), offer.getMarket().getBaseCurrencyCode());
        Optional<Monetary> baseMax = AmountSpecUtil.findBaseSideMaxOrFixedAmountFromSpec(
                offer.getAmountSpec(), offer.getMarket().getBaseCurrencyCode());
        if (baseMin.isPresent() && baseMax.isPresent()) {
            if (baseSideAmount < baseMin.get().getValue() || baseSideAmount > baseMax.get().getValue()) {
                throw reject("The contract base amount lies outside the offer's amount spec. base="
                        + baseSideAmount + ", spec min=" + baseMin.get().getValue() + ", max=" + baseMax.get().getValue());
            }
            return true;
        }
        Optional<Monetary> quoteMin = AmountSpecUtil.findQuoteSideMinOrFixedAmountFromSpec(
                offer.getAmountSpec(), offer.getMarket().getQuoteCurrencyCode());
        Optional<Monetary> quoteMax = AmountSpecUtil.findQuoteSideMaxOrFixedAmountFromSpec(
                offer.getAmountSpec(), offer.getMarket().getQuoteCurrencyCode());
        if (quoteMin.isPresent() && quoteMax.isPresent()) {
            if (quoteSideAmount < quoteMin.get().getValue() || quoteSideAmount > quoteMax.get().getValue()) {
                throw reject("The contract quote amount lies outside the offer's amount spec. quote="
                        + quoteSideAmount + ", spec min=" + quoteMin.get().getValue() + ", max=" + quoteMax.get().getValue());
            }
            return false;
        }
        throw reject("The offer's amount spec pins neither side");
    }

    // The side not pinned by the amount spec must be consistent with the offer's price spec
    // resolved at the maker's current market price, within the maker's configured tolerance.
    // Whether a deviation is adverse depends on whether the maker gives or receives the derived
    // asset. Offer.direction is the maker's raw BASE-currency direction (getDisplayDirection is
    // the one that mirrors it for BTC-quote markets), so it is exactly whether the maker buys the
    // base side - no market-dependent flip.
    private static void validatePriceTolerance(MarketPriceService marketPriceService,
                                               SettingsService settingsService,
                                               MuSigOffer offer,
                                               boolean specIsBaseSide,
                                               long baseSideAmount,
                                               long quoteSideAmount) {
        Market market = offer.getMarket();
        PriceSpec priceSpec = offer.getPriceSpec();
        PriceQuote quote;
        if (priceSpec instanceof FixPriceSpec) {
            // A fixed-price offer carries its own price; no market lookup or freshness check.
            quote = ((FixPriceSpec) priceSpec).getPriceQuote();
        } else {
            // Market- and float-price offers resolve against the live market price. Read ONE
            // MarketPrice snapshot and derive both the freshness decision and the quote from it:
            // the service replaces map entries concurrently, so a second lookup could pass
            // freshness on a different object than the quote came from. It also keeps the previous
            // map after a failed refresh and delegates freshness to callers, so a >12h-stale quote
            // must be rejected rather than trusted.
            Optional<MarketPrice> marketPrice = marketPriceService.findMarketPrice(market);
            if (marketPrice.isEmpty() || !marketPrice.get().isValidDate()) {
                throw reject("No fresh market price is available to validate the contract amounts. market=" + market);
            }
            PriceQuote marketQuote = marketPrice.get().getPriceQuote();
            if (priceSpec instanceof FloatPriceSpec) {
                quote = PriceUtil.fromMarketPriceMarkup(marketQuote, ((FloatPriceSpec) priceSpec).getPercentage());
            } else if (priceSpec instanceof MarketPriceSpec) {
                quote = marketQuote;
            } else {
                throw reject("Unsupported price spec for validation. priceSpec=" + priceSpec);
            }
        }
        if (quote.getValue() <= 0) {
            // A zero or negative price cannot validate amounts: it would zero the cross-multiplication
            // denominator (or numerator) and fail the tolerance check open.
            throw reject("The resolved price is not positive; cannot validate the contract amounts. value="
                    + quote.getValue());
        }
        double tolerance = settingsService.getMaxTradePriceDeviation().get();
        // The free side must match the offer's price within tolerance in BOTH directions. Bounding
        // only the adverse side would let an unboundedly favorable side through: for a BTC/fiat
        // maker who receives the fiat side, a taker could inflate the fiat amount far above the
        // Bitcoin value, passing a one-sided check while the rail limit - valued on the Bitcoin
        // side - stays under its cap, so the maker co-signs a fiat trade well above the rail limit.
        // The expected free-side amount is the rational numerator/denominator; PriceQuote's
        // toQuoteSideMonetary/toBaseSideMonetary truncate that to a long and wrap on overflow, so
        // compare by cross-multiplication (both denominators are positive) - exact, no division.
        java.math.BigDecimal numerator;
        java.math.BigDecimal denominator;
        long actual;
        if (specIsBaseSide) {
            // Mirror toQuoteSideMonetary: base.value * price.value / 10^base.precision.
            Monetary base = Monetary.from(baseSideAmount, market.getBaseCurrencyCode());
            numerator = java.math.BigDecimal.valueOf(base.getValue())
                    .multiply(java.math.BigDecimal.valueOf(quote.getValue()));
            denominator = java.math.BigDecimal.TEN.pow(base.getPrecision());
            actual = quoteSideAmount;
        } else {
            // Mirror toBaseSideMonetary: quote.value * 10^(price base precision) / price.value.
            numerator = java.math.BigDecimal.valueOf(quoteSideAmount)
                    .multiply(java.math.BigDecimal.TEN.pow(quote.getBaseSideMonetary().getPrecision()));
            denominator = java.math.BigDecimal.valueOf(quote.getValue());
            actual = baseSideAmount;
        }
        // lower*expected <= actual <= upper*expected  <=>  (cross-multiplying by the positive
        // denominator)  numerator*(1-tol) <= actual*denominator <= numerator*(1+tol).
        java.math.BigDecimal actualTimesDenominator = java.math.BigDecimal.valueOf(actual).multiply(denominator);
        java.math.BigDecimal lowerBound = numerator.multiply(java.math.BigDecimal.ONE.subtract(java.math.BigDecimal.valueOf(tolerance)));
        java.math.BigDecimal upperBound = numerator.multiply(java.math.BigDecimal.ONE.add(java.math.BigDecimal.valueOf(tolerance)));
        if (actualTimesDenominator.compareTo(lowerBound) < 0 || actualTimesDenominator.compareTo(upperBound) > 0) {
            throw rejectPriceDeviation("The contract amounts deviate from the offer price beyond the tolerance. actual="
                    + actual + ", expected≈" + expectedForLog(numerator, denominator));
        }
    }

    // The decision uses exact cross-multiplication; this rounded quotient is only for the log line.
    private static String expectedForLog(java.math.BigDecimal numerator, java.math.BigDecimal denominator) {
        return numerator.divide(denominator, java.math.MathContext.DECIMAL64).toPlainString();
    }

    private static void validateUsdLimits(MarketPriceService marketPriceService,
                                          MuSigContract contract,
                                          Market market,
                                          long baseSideAmount,
                                          long quoteSideAmount) {
        long btcSideValue = market.isBaseCurrencyBitcoin() ? baseSideAmount : quoteSideAmount;
        if (btcSideValue > MAX_BITCOIN_SUPPLY_SATS) {
            throw reject("The Bitcoin-side amount exceeds the total supply. sats=" + btcSideValue);
        }
        Coin btcAmount = Coin.asBtcFromValue(btcSideValue);
        Optional<MarketPrice> btcUsdMarketPrice = marketPriceService.findMarketPrice(MarketRepository.getUSDBitcoinMarket());
        if (btcUsdMarketPrice.isEmpty() || !btcUsdMarketPrice.get().isValidDate()) {
            // A stale BTC/USD price (kept from a failed refresh) would misvalue the trade against
            // the absolute and rail limits, so require a fresh one before enforcing them.
            throw reject("No fresh BTC/USD price is available to validate the absolute trade limits");
        }
        // Compute the USD value with exact arithmetic and compare in BigDecimal: the supply cap
        // alone does not prevent PriceQuote's toQuoteSideMonetary from wrapping its longValue at
        // an extreme BTC/USD price, which would make an enormous amount read as a tiny one. The
        // result is a USD Fiat atomic value, so it compares directly with the policy limits.
        java.math.BigDecimal usdAtomic = java.math.BigDecimal.valueOf(btcAmount.getValue())
                .multiply(java.math.BigDecimal.valueOf(btcUsdMarketPrice.get().getPriceQuote().getValue()))
                .movePointLeft(btcAmount.getPrecision());
        if (usdAtomic.compareTo(java.math.BigDecimal.valueOf(MuSigTradeAmountLimitsPolicy.MIN_USD_TRADE_AMOUNT.getValue())) < 0) {
            throw reject("The trade amount lies below the absolute minimum. usd=" + usdAtomic.toPlainString());
        }
        // The payment-rail cap limits the fiat obligation, which is the non-Bitcoin side. The
        // two-sided price tolerance lets that side sit up to the tolerance above the Bitcoin value,
        // so valuing the cap on the Bitcoin side alone would let the fiat obligation exceed the cap
        // by the tolerance. When the fiat side is quoted in USD it is directly comparable in the
        // same atomic scale, so cap on the larger of the Bitcoin-derived value and the actual USD
        // obligation. (Non-USD fiat still needs a fiat/USD conversion - a disclosed follow-up.)
        java.math.BigDecimal obligationUsd = usdAtomic;
        if (market.isBaseCurrencyBitcoin() && "USD".equals(market.getQuoteCurrencyCode())) {
            obligationUsd = obligationUsd.max(java.math.BigDecimal.valueOf(quoteSideAmount));
        }
        PaymentMethodSpec<?> nonBtcSideSpec = market.isBaseCurrencyBitcoin()
                ? contract.getQuoteSidePaymentMethodSpec()
                : contract.getBaseSidePaymentMethodSpec();
        Fiat railLimit = MuSigTradeAmountLimitsPolicy.getMaxTradeLimitInUsd(
                nonBtcSideSpec.getPaymentMethod().getPaymentRail());
        if (obligationUsd.compareTo(java.math.BigDecimal.valueOf(railLimit.getValue())) > 0) {
            throw reject("The trade amount exceeds the payment rail's limit. usd=" + obligationUsd.toPlainString()
                    + ", limit=" + railLimit.getValue());
        }
    }

    // The dispute agents are contract terms the taker must not choose: the maker re-runs the
    // same deterministic selections and requires agreement.
    private static void validateDisputeAgents(MuSigTraderMediationService mediationService,
                                              MuSigTraderArbitrationService arbitrationService,
                                              MuSigContract contract,
                                              MuSigOffer offer) {
        if (!DevMode.isDevMode() && (contract.getMediator().isEmpty() || contract.getArbitrator().isEmpty())) {
            // The taker's own creation path rejects missing dispute agents outside dev mode;
            // the maker enforces the same invariant, else a crafted contract could create a
            // trade whose mediation and arbitration requests have no recipient.
            throw reject("The contract names no mediator or no arbitrator");
        }
        String makersUserProfileId = offer.getMakersUserProfileId();
        String takersUserProfileId = contract.getTaker().getNetworkId().getId();
        Optional<UserProfile> myMediator = mediationService.selectMediator(makersUserProfileId,
                takersUserProfileId,
                offer.getId());
        if (!sameDisputeAgent(myMediator, contract.getMediator())) {
            log.warn("Rejecting the MuSig take offer request before trade creation: mediators do not match. mine={}, contract={}",
                    myMediator.map(p -> StringUtils.sanitizeForLog(p.getNickName())), contract.getMediator().map(p -> StringUtils.sanitizeForLog(p.getNickName())));
            throw new TradeProtocolException("The maker has rejected the take offer request because the mediators do not match.",
                    TradeProtocolFailure.MEDIATORS_NOT_MATCHING);
        }
        Optional<UserProfile> myArbitrator = arbitrationService.selectArbitrator(makersUserProfileId,
                takersUserProfileId,
                offer.getId(),
                myMediator.map(UserProfile::getId));
        if (!sameDisputeAgent(myArbitrator, contract.getArbitrator())) {
            log.warn("Rejecting the MuSig take offer request before trade creation: arbitrators do not match. mine={}, contract={}",
                    myArbitrator.map(p -> StringUtils.sanitizeForLog(p.getNickName())), contract.getArbitrator().map(p -> StringUtils.sanitizeForLog(p.getNickName())));
            throw new TradeProtocolException("The maker has rejected the take offer request because the arbitrators do not match.",
                    TradeProtocolFailure.MEDIATORS_NOT_MATCHING);
        }
    }

    // The dispute-agent profile is retained in the signed contract and shown as the assigned
    // agent, so it must equal the maker's own selected profile, not merely share its network id.
    // UserProfile.equals covers nickname, proof of work, network id, terms and statement but not
    // avatarVersion, which is attacker-controlled and breaks avatar rendering for an unsupported
    // value, so it is compared explicitly; the remaining version fields stay excluded so a benign
    // profile-version skew does not reject a genuine agent.
    private static boolean sameDisputeAgent(Optional<UserProfile> mine, Optional<UserProfile> theirs) {
        // UserProfile.equals omits version, applicationVersion and avatarVersion, yet all three are
        // part of the contract the maker co-signs (the version field even selects which fields
        // serializeForHash clears). Compare equals plus those three fields - i.e. the whole profile
        // - so a taker cannot forge dispute-agent profile metadata into the signed contract while
        // this check still accepts it.
        return mine.equals(theirs)
                && mine.map(UserProfile::getVersion).equals(theirs.map(UserProfile::getVersion))
                && mine.map(UserProfile::getApplicationVersion).equals(theirs.map(UserProfile::getApplicationVersion))
                && mine.map(UserProfile::getAvatarVersion).equals(theirs.map(UserProfile::getAvatarVersion));
    }

    private static TradeProtocolException rejectPriceDeviation(String logMessage) {
        log.warn("Rejecting the MuSig take offer request before trade creation: {}", logMessage);
        return new TradeProtocolException(
                "The maker has rejected the take offer request because the amounts deviate too much from the current market price.",
                TradeProtocolFailure.PRICE_DEVIATION);
    }


    private static TradeProtocolException reject(String logMessage) {
        log.warn("Rejecting the MuSig take offer request before trade creation: {}", logMessage);
        return new TradeProtocolException(
                "The maker has rejected the take offer request because the offer is not available anymore.",
                TradeProtocolFailure.OFFER_NOT_AVAILABLE);
    }
}
