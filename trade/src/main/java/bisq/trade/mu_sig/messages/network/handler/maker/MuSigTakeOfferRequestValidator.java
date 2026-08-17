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

import bisq.account.payment_method.PaymentMethodSpec;
import bisq.account.protocol_type.TradeProtocolType;
import bisq.bonded_roles.market_price.MarketPrice;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.application.DevMode;
import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.common.monetary.Coin;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.PriceQuote;
import bisq.common.util.StringUtils;
import bisq.contract.ContractService;
import bisq.contract.ContractSignatureData;
import bisq.contract.Role;
import bisq.contract.mu_sig.MuSigContract;
import bisq.network.identity.NetworkId;
import bisq.offer.amount.spec.AmountSpecUtil;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.mu_sig.MuSigTradeAmountLimitsPolicy;
import bisq.offer.mu_sig.MyMuSigOffersService;
import bisq.offer.price.PriceUtil;
import bisq.offer.price.spec.FixPriceSpec;
import bisq.offer.price.spec.FloatPriceSpec;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.offer.price.spec.PriceSpec;
import bisq.settings.SettingsService;
import bisq.trade.Trade;
import bisq.trade.exceptions.TradeProtocolException;
import bisq.trade.exceptions.TradeProtocolFailure;
import bisq.trade.mu_sig.arbitration.MuSigTraderArbitrationService;
import bisq.trade.mu_sig.mediation.MuSigTraderMediationService;
import bisq.trade.mu_sig.messages.network.SetupTradeMessage_A;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.Optional;

import static bisq.common.validation.NetworkDataValidation.TWO_HOURS;

/**
 * Validates an incoming take offer request before any trade is created or persisted.
 * Structural and identity rejections carry the same generic wire reason while the log keeps
 * the specific cause, so a crafted request learns nothing from the failure mode; price
 * deviations and dispute-agent mismatches carry their specific reasons, as those are
 * legitimate conditions an honest taker can act on.
 */
@Slf4j
public final class MuSigTakeOfferRequestValidator {
    // Also keeps the USD-limit conversion below the range where PriceQuote's long arithmetic wraps.
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
            // On a multi-identity node the confidential layer only binds the receiving identity.
            throw reject("The message receiver does not match the offer's maker network id. receiverId="
                    + StringUtils.sanitizeForLog(message.getReceiver().getId()) + ", offer makerId=" + StringUtils.sanitizeForLog(contract.getOffer().getMakerNetworkId().getId()));
        }
        String expectedTradeId = Trade.createId(contract.getOffer().getId(),
                takerNetworkId.getId(),
                contract.getTakeOfferDate());
        if (!message.getTradeId().equals(expectedTradeId)) {
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
            // Any failure mode of a crafted signature must reject, not crash.
            throw reject("The taker's contract signature could not be verified: " + e.getMessage());
        }
        if (!signatureValid) {
            throw reject("The taker's contract signature does not verify");
        }
    }

    public static void validateTakerProfileKnown(UserProfileService userProfileService, SetupTradeMessage_A message) {
        // The handler resolves the profile with orElseThrow after the trade exists.
        if (userProfileService.findUserProfile(message.getSender().getId()).isEmpty()) {
            throw reject("The taker's user profile is not known");
        }
    }

    public static void validateOffer(MyMuSigOffersService myMuSigOffersService, SetupTradeMessage_A message) {
        MuSigOffer embeddedOffer = message.getContract().getOffer();
        // Only the activated set establishes takeability. No fallback to existing trades: that
        // would let a cached copy of a removed offer be replayed.
        Optional<MuSigOffer> myOffer = myMuSigOffersService.findActivatedOffer(embeddedOffer.getId());
        if (myOffer.isEmpty()) {
            throw reject("The offer is not one of the maker's activated offers. offerId="
                    + StringUtils.sanitizeForLog(embeddedOffer.getId()));
        }
        if (!myOffer.get().equals(embeddedOffer)) {
            throw reject("The embedded offer does not equal the maker's own offer with that id. offerId="
                    + StringUtils.sanitizeForLog(embeddedOffer.getId()));
        }
        if (!myOffer.get().toProto(true).equals(embeddedOffer.toProto(true))) {
            // Offer.equals ignores fields that are still hash-relevant (e.g. the Market currency
            // names); the serialize-for-hash form covers them while ignoring the excluded version fields.
            throw reject("The embedded offer differs from the maker's own offer in a hash-relevant field. offerId="
                    + StringUtils.sanitizeForLog(embeddedOffer.getId()));
        }
        // Contract.verify only checks the date, so pin the taker-authored discriminators.
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
        // The taker-chosen take date drives the trade id and UI time; allow the same clock-skew
        // window as generic network dates, but not an older backdate.
        if (contract.getTakeOfferDate() + TWO_HOURS < offer.getDate()) {
            throw reject("The take offer date predates the offer beyond the clock-skew window. takeOfferDate=" + contract.getTakeOfferDate()
                    + ", offerDate=" + offer.getDate());
        }
        long baseSideAmount = contract.getBaseSideAmount();
        long quoteSideAmount = contract.getQuoteSideAmount();
        if (baseSideAmount <= 0 || quoteSideAmount <= 0) {
            throw reject("Non-positive contract amounts. base=" + baseSideAmount + ", quote=" + quoteSideAmount);
        }
        if (!contract.getPriceSpec().equals(offer.getPriceSpec())) {
            throw reject("The contract price spec does not equal the offer's price spec");
        }
        if (contract.getTaker().getSaltedAccountPayloadHash().map(hash -> hash.length == 0).orElse(true)) {
            // Optional on the wire and only validated when present; its absence would otherwise
            // surface after the maker signs the deposit transaction.
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

    // Returns whether the spec pins the base side, so the tolerance check knows which side is derived.
    private static boolean validateAmountSpecMembership(MuSigOffer offer, long baseSideAmount, long quoteSideAmount) {
        Optional<Monetary> baseMin = AmountSpecUtil.findBaseSideMinOrFixedAmountFromSpec(
                offer.getAmountSpec(), offer.getMarket().getBaseCurrencyCode());
        Optional<Monetary> baseMax = AmountSpecUtil.findBaseSideMaxOrFixedAmountFromSpec(
                offer.getAmountSpec(), offer.getMarket().getBaseCurrencyCode());
        if (baseMin.isPresent() && baseMax.isPresent()) {
            long min = baseMin.get().getValue();
            long max = baseMax.get().getValue();
            if ((baseSideAmount < min || baseSideAmount > max)
                    && !isWithinWholeFiatRoundingOfBaseBoundary(offer.getMarket(), baseSideAmount,
                    quoteSideAmount, min, max)) {
                throw reject("The contract base amount lies outside the offer's amount spec. base="
                        + baseSideAmount + ", spec min=" + min + ", max=" + max);
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

    private static boolean isWithinWholeFiatRoundingOfBaseBoundary(Market market,
                                                                    long baseSideAmount,
                                                                    long quoteSideAmount,
                                                                    long min,
                                                                    long max) {
        if (!market.isBtcFiatMarket()) {
            return false;
        }
        long wholeFiatUnit = Fiat.fromFaceValue(1, market.getQuoteCurrencyCode()).getValue();
        if (quoteSideAmount % wholeFiatUnit != 0) {
            return false;
        }
        long maxQuoteRoundingDelta = wholeFiatUnit / 2 + 1;
        if (quoteSideAmount <= maxQuoteRoundingDelta) {
            return false;
        }
        long boundary = baseSideAmount < min ? min : max;
        BigDecimal distance = BigDecimal.valueOf(baseSideAmount)
                .subtract(BigDecimal.valueOf(boundary))
                .abs();
        BigDecimal maxRoundingDistance = BigDecimal.valueOf(boundary)
                .multiply(BigDecimal.valueOf(maxQuoteRoundingDelta))
                .divide(BigDecimal.valueOf(quoteSideAmount - maxQuoteRoundingDelta), 0, RoundingMode.CEILING)
                .add(BigDecimal.ONE);
        return distance.compareTo(maxRoundingDistance) <= 0;
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
            quote = ((FixPriceSpec) priceSpec).getPriceQuote();
        } else {
            // Read ONE MarketPrice snapshot for both the freshness decision and the quote (the map
            // is replaced concurrently) and reject a stale quote (freshness is delegated to callers).
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
            // A non-positive price would zero the cross-multiplication and fail the tolerance check open.
            throw reject("The resolved price is not positive; cannot validate the contract amounts. value="
                    + quote.getValue());
        }
        double tolerance = settingsService.getMaxTradePriceDeviation().get();
        // Bound the free side in BOTH directions: an unbounded favorable side would let the fiat
        // obligation exceed the rail cap. Compare by cross-multiplication (positive denominators)
        // rather than PriceQuote's monetary conversions, whose longValue wraps on overflow.
        BigDecimal numerator;
        BigDecimal denominator;
        long actual;
        if (specIsBaseSide) {
            // Mirror toQuoteSideMonetary: base.value * price.value / 10^base.precision.
            Monetary base = Monetary.from(baseSideAmount, market.getBaseCurrencyCode());
            numerator = BigDecimal.valueOf(base.getValue())
                    .multiply(BigDecimal.valueOf(quote.getValue()));
            denominator = BigDecimal.TEN.pow(base.getPrecision());
            actual = quoteSideAmount;
        } else {
            // Mirror toBaseSideMonetary: quote.value * 10^(price base precision) / price.value.
            numerator = BigDecimal.valueOf(quoteSideAmount)
                    .multiply(BigDecimal.TEN.pow(quote.getBaseSideMonetary().getPrecision()));
            denominator = BigDecimal.valueOf(quote.getValue());
            actual = baseSideAmount;
        }
        // lower*expected <= actual <= upper*expected  <=>  (cross-multiplying by the positive
        // denominator)  numerator*(1-tol) <= actual*denominator <= numerator*(1+tol).
        BigDecimal actualTimesDenominator = BigDecimal.valueOf(actual).multiply(denominator);
        BigDecimal lowerBound = numerator.multiply(BigDecimal.ONE.subtract(BigDecimal.valueOf(tolerance)));
        BigDecimal upperBound = numerator.multiply(BigDecimal.ONE.add(BigDecimal.valueOf(tolerance)));
        if (actualTimesDenominator.compareTo(lowerBound) < 0 || actualTimesDenominator.compareTo(upperBound) > 0) {
            throw rejectPriceDeviation("The contract amounts deviate from the offer price beyond the tolerance. actual="
                    + actual + ", expected≈" + expectedForLog(numerator, denominator));
        }
    }

    // The decision uses exact cross-multiplication; this rounded quotient is only for the log line.
    private static String expectedForLog(BigDecimal numerator, BigDecimal denominator) {
        return numerator.divide(denominator, MathContext.DECIMAL64).toPlainString();
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
        // Exact BigDecimal (PriceQuote's longValue would wrap at an extreme price); the result is
        // a USD Fiat atomic value comparable to the policy limits.
        BigDecimal usdAtomic = BigDecimal.valueOf(btcAmount.getValue())
                .multiply(BigDecimal.valueOf(btcUsdMarketPrice.get().getPriceQuote().getValue()))
                .movePointLeft(btcAmount.getPrecision());
        if (usdAtomic.compareTo(BigDecimal.valueOf(MuSigTradeAmountLimitsPolicy.MIN_USD_TRADE_AMOUNT.getValue())) < 0) {
            throw reject("The trade amount lies below the absolute minimum. usd=" + usdAtomic.toPlainString());
        }
        // The rail cap must bound the actual fiat obligation, which the tolerance lets sit above
        // the Bitcoin value. For a USD quote that obligation is directly comparable, so cap on the
        // larger of the two. (Non-USD fiat needs a fiat/USD conversion - a disclosed follow-up.)
        BigDecimal obligationUsd = usdAtomic;
        if (market.isBaseCurrencyBitcoin() && "USD".equals(market.getQuoteCurrencyCode())) {
            obligationUsd = obligationUsd.max(BigDecimal.valueOf(quoteSideAmount));
        }
        PaymentMethodSpec<?> nonBtcSideSpec = market.isBaseCurrencyBitcoin()
                ? contract.getQuoteSidePaymentMethodSpec()
                : contract.getBaseSidePaymentMethodSpec();
        Fiat railLimit = MuSigTradeAmountLimitsPolicy.getMaxTradeLimitInUsd(
                nonBtcSideSpec.getPaymentMethod().getPaymentRail());
        if (obligationUsd.compareTo(BigDecimal.valueOf(railLimit.getValue())) > 0) {
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

    // The maker co-signs the taker's copy of the dispute-agent profile, so compare the whole
    // profile: equals plus the three version fields it omits (the version field even selects which
    // fields serializeForHash clears), else a taker could forge that metadata into the contract.
    private static boolean sameDisputeAgent(Optional<UserProfile> mine, Optional<UserProfile> theirs) {
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
