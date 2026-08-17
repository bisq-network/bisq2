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
import bisq.account.payment_method.PaymentMethodSpecUtil;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.common.market.Market;
import bisq.common.monetary.Coin;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.PriceQuote;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.ClearnetAddress;
import bisq.common.network.TransportType;
import bisq.contract.ContractService;
import bisq.contract.ContractSignatureData;
import bisq.contract.mu_sig.MuSigContract;
import bisq.network.identity.NetworkId;
import bisq.offer.Direction;
import bisq.offer.amount.spec.BaseSideFixedAmountSpec;
import bisq.offer.amount.spec.BaseSideRangeAmountSpec;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.mu_sig.MyMuSigOffersService;
import bisq.offer.price.spec.FixPriceSpec;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.trade.Trade;
import bisq.trade.exceptions.TradeProtocolException;
import bisq.trade.exceptions.TradeProtocolFailure;
import bisq.trade.mu_sig.messages.network.SetupTradeMessage_A;
import bisq.trade.mu_sig.messages.network.mu_sig_data.PubKeyShares;
import bisq.user.profile.UserProfile;
import org.junit.jupiter.api.Test;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static bisq.common.validation.NetworkDataValidation.TWO_HOURS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MuSigTakeOfferRequestValidatorTest {
    private static final KeyPair TAKER_KEY_PAIR = KeyGeneration.generateDefaultEcKeyPair();
    private static final NetworkId OFFER_MAKER_NETWORK_ID = createNetworkId("offer-maker", 9997, null);
    private static final NetworkId TAKER_NETWORK_ID = createNetworkId("taker", 9999, TAKER_KEY_PAIR);
    private static final NetworkId OTHER_NETWORK_ID = createNetworkId("other", 9996, null);

    private final ContractService contractService = new ContractService(null);
    private final bisq.bonded_roles.market_price.MarketPriceService marketPriceService =
            org.mockito.Mockito.mock(bisq.bonded_roles.market_price.MarketPriceService.class);
    private final bisq.settings.SettingsService settingsService =
            org.mockito.Mockito.mock(bisq.settings.SettingsService.class);
    private final bisq.trade.mu_sig.mediation.MuSigTraderMediationService mediationService =
            org.mockito.Mockito.mock(bisq.trade.mu_sig.mediation.MuSigTraderMediationService.class);
    private final bisq.trade.mu_sig.arbitration.MuSigTraderArbitrationService arbitrationService =
            org.mockito.Mockito.mock(bisq.trade.mu_sig.arbitration.MuSigTraderArbitrationService.class);

    private final UserProfile mediatorProfile = org.mockito.Mockito.mock(UserProfile.class);
    private final UserProfile arbitratorProfile = org.mockito.Mockito.mock(UserProfile.class);

    @org.junit.jupiter.api.BeforeEach
    void setUpEconomicsMocks() {
        org.mockito.Mockito.when(mediatorProfile.getNetworkId()).thenReturn(createNetworkId("mediator", 9990, null));
        org.mockito.Mockito.when(mediatorProfile.getUserName()).thenReturn("mediator");
        org.mockito.Mockito.when(arbitratorProfile.getNetworkId()).thenReturn(createNetworkId("arbitrator", 9989, null));
        org.mockito.Mockito.when(arbitratorProfile.getUserName()).thenReturn("arbitrator");
        org.mockito.Mockito.when(settingsService.getMaxTradePriceDeviation())
                .thenReturn(new bisq.common.observable.Observable<>(0.05));
        // A fresh BTC/USD market price for the absolute USD limit conversions and for resolving
        // market-price offers; findMarketPrice carries the timestamp the freshness check reads.
        bisq.bonded_roles.market_price.MarketPrice freshPrice =
                org.mockito.Mockito.mock(bisq.bonded_roles.market_price.MarketPrice.class);
        org.mockito.Mockito.when(freshPrice.isValidDate()).thenReturn(true);
        org.mockito.Mockito.when(freshPrice.getPriceQuote()).thenReturn(PriceQuote.fromFiatPrice(50_000, "USD"));
        org.mockito.Mockito.when(marketPriceService.findMarketPrice(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Optional.of(freshPrice));
        org.mockito.Mockito.when(mediationService.selectMediator(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(mediatorProfile));
        org.mockito.Mockito.when(arbitrationService.selectArbitrator(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn(Optional.of(arbitratorProfile));
    }

    @Test
    void validRequestPasses() throws GeneralSecurityException {
        MuSigContract contract = createContract();
        SetupTradeMessage_A message = createMessage(contract, TAKER_NETWORK_ID, sign(contract, TAKER_KEY_PAIR));

        assertThatCode(() -> MuSigTakeOfferRequestValidator.validateIdentity(contractService, message))
                .doesNotThrowAnyException();
    }

    @Test
    void senderNotMatchingContractTakerIsRejected() throws GeneralSecurityException {
        MuSigContract contract = createContract();
        SetupTradeMessage_A message = createMessage(contract, OTHER_NETWORK_ID, sign(contract, TAKER_KEY_PAIR));

        assertRejected(message);
    }

    @Test
    void signatureFromForeignKeyIsRejected() throws GeneralSecurityException {
        MuSigContract contract = createContract();
        KeyPair attackerKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        // The signature is internally consistent - hash and key match the signature - but the
        // key is not the taker's; without the binding check the maker would accept it.
        SetupTradeMessage_A message = createMessage(contract, TAKER_NETWORK_ID, sign(contract, attackerKeyPair));

        assertRejected(message);
    }

    @Test
    void tamperedSignatureIsRejected() throws GeneralSecurityException {
        MuSigContract contract = createContract();
        ContractSignatureData signatureData = sign(contract, TAKER_KEY_PAIR);
        byte[] tamperedSignature = signatureData.getSignature().clone();
        // Flip a bit in the middle of the signature bits, not the DER framing: the verifier
        // must return false on a well-formed but incorrect signature, not merely throw on a
        // malformed encoding.
        tamperedSignature[tamperedSignature.length / 2] ^= 0x01;
        ContractSignatureData tampered = new ContractSignatureData(signatureData.getContractHash(),
                tamperedSignature,
                signatureData.getPublicKey());
        SetupTradeMessage_A message = createMessage(contract, TAKER_NETWORK_ID, tampered);

        assertRejected(message);
    }

    @Test
    void receiverNotMatchingOfferMakerIsRejected() throws GeneralSecurityException {
        MuSigContract contract = createContract();
        SetupTradeMessage_A message = createMessage(contract, TAKER_NETWORK_ID, OTHER_NETWORK_ID,
                sign(contract, TAKER_KEY_PAIR),
                Trade.createId(contract.getOffer().getId(),
                        contract.getTaker().getNetworkId().getId(),
                        contract.getTakeOfferDate()));

        assertRejected(message);
    }

    @Test
    void tradeIdNotDerivedFromContractIsRejected() throws GeneralSecurityException {
        MuSigContract contract = createContract();
        // The contract itself is validly signed; only the claimed trade id differs from the id
        // the maker derives, which would otherwise persist a failed trade per crafted message.
        SetupTradeMessage_A message = createMessage(contract, TAKER_NETWORK_ID, OFFER_MAKER_NETWORK_ID,
                sign(contract, TAKER_KEY_PAIR), "crafted-trade-id");

        assertRejected(message);
    }

    @Test
    void activatedOwnOfferPasses() throws GeneralSecurityException {
        MuSigContract contract = createContract();
        SetupTradeMessage_A message = createMessage(contract, TAKER_NETWORK_ID, sign(contract, TAKER_KEY_PAIR));
        MyMuSigOffersService myOffers = org.mockito.Mockito.mock(MyMuSigOffersService.class);
        org.mockito.Mockito.when(myOffers.findActivatedOffer("test-id"))
                .thenReturn(Optional.of(contract.getOffer()));

        assertThatCode(() -> MuSigTakeOfferRequestValidator.validateOffer(myOffers, message))
                .doesNotThrowAnyException();
    }

    @Test
    void unknownOrDeactivatedOfferIsRejected() throws GeneralSecurityException {
        MuSigContract contract = createContract();
        SetupTradeMessage_A message = createMessage(contract, TAKER_NETWORK_ID, sign(contract, TAKER_KEY_PAIR));
        // findActivatedOffer covers both cases: an unknown offer and a retained-but-deactivated
        // offer are equally absent from the activated set.
        MyMuSigOffersService myOffers = org.mockito.Mockito.mock(MyMuSigOffersService.class);
        org.mockito.Mockito.when(myOffers.findActivatedOffer("test-id")).thenReturn(Optional.empty());

        assertOfferRejected(myOffers, message);
    }

    @Test
    void embeddedOfferDifferingFromOwnOfferIsRejected() throws GeneralSecurityException {
        MuSigContract contract = createContract();
        SetupTradeMessage_A message = createMessage(contract, TAKER_NETWORK_ID, sign(contract, TAKER_KEY_PAIR));
        // Same id, different terms: the taker altered the embedded offer while keeping the id
        // of a genuine activated offer.
        MuSigOffer myRealOffer = createOffer(new BaseSideFixedAmountSpec(999_999L));
        MyMuSigOffersService myOffers = org.mockito.Mockito.mock(MyMuSigOffersService.class);
        org.mockito.Mockito.when(myOffers.findActivatedOffer("test-id"))
                .thenReturn(Optional.of(myRealOffer));

        assertOfferRejected(myOffers, message);
    }

    @Test
    void contractWithForgedProtocolTypeIsRejected() throws GeneralSecurityException {
        // Contract.verify checks only the date, so a taker can serialize a MuSigContract that
        // declares BISQ_EASY; MuSigContract.fromProto keeps the forged value. The maker must not
        // double-sign it. The wire mutation is reproduced by round-tripping through proto.
        MuSigContract genuine = createContract();
        bisq.contract.protobuf.Contract forgedProto = genuine.toProto(false).toBuilder()
                .setTradeProtocolType(bisq.account.protocol_type.TradeProtocolType.BISQ_EASY.toProtoEnum())
                .build();
        MuSigContract forged = MuSigContract.fromProto(forgedProto);
        SetupTradeMessage_A message = createMessage(forged, TAKER_NETWORK_ID, sign(genuine, TAKER_KEY_PAIR));
        MyMuSigOffersService myOffers = org.mockito.Mockito.mock(MyMuSigOffersService.class);
        org.mockito.Mockito.when(myOffers.findActivatedOffer("test-id"))
                .thenReturn(Optional.of(forged.getOffer()));

        assertOfferRejected(myOffers, message);
    }

    @Test
    void contractWithForgedTakerRoleIsRejected() throws GeneralSecurityException {
        // A taker can serialize its own Party with role MAKER; verification never checks it.
        MuSigContract genuine = createContract();
        bisq.contract.protobuf.Contract proto = genuine.toProto(false);
        bisq.contract.protobuf.Contract forgedProto = proto.toBuilder()
                .setTwoPartyContract(proto.getTwoPartyContract().toBuilder()
                        .setTaker(proto.getTwoPartyContract().getTaker().toBuilder()
                                .setRole(bisq.contract.protobuf.Role.ROLE_MAKER)))
                .build();
        MuSigContract forged = MuSigContract.fromProto(forgedProto);
        SetupTradeMessage_A message = createMessage(forged, TAKER_NETWORK_ID, sign(genuine, TAKER_KEY_PAIR));
        MyMuSigOffersService myOffers = org.mockito.Mockito.mock(MyMuSigOffersService.class);
        org.mockito.Mockito.when(myOffers.findActivatedOffer("test-id"))
                .thenReturn(Optional.of(forged.getOffer()));

        assertOfferRejected(myOffers, message);
    }

    @Test
    void embeddedOfferWithAForgedCurrencyNameIsRejected() throws GeneralSecurityException {
        // Market.equals excludes the currency names, but they are part of the contract hash (not
        // @ExcludeForHash). Starting from the maker's own offer, the taker forges only the base
        // currency name; equals still passes (names excluded, same date and economic fields),
        // yet the maker would co-sign a contract committing to the forged name. The
        // serialize-for-hash comparison rejects it.
        MuSigOffer makersOffer = createOffer(new BaseSideFixedAmountSpec(2_000_000L));
        bisq.offer.protobuf.Offer forgedProto = makersOffer.toProto(false).toBuilder()
                .setMarket(makersOffer.toProto(false).getMarket().toBuilder().setBaseCurrencyName("Forged"))
                .build();
        MuSigOffer forged = MuSigOffer.fromProto(forgedProto);
        MuSigContract contract = createContract(forged, 2_000_000L, 10_000_000L, forged.getPriceSpec(),
                Optional.empty(), Optional.empty());
        SetupTradeMessage_A message = createMessage(contract, TAKER_NETWORK_ID, sign(contract, TAKER_KEY_PAIR));
        MyMuSigOffersService myOffers = org.mockito.Mockito.mock(MyMuSigOffersService.class);
        org.mockito.Mockito.when(myOffers.findActivatedOffer("test-id")).thenReturn(Optional.of(makersOffer));

        assertOfferRejected(myOffers, message);
    }

    @Test
    void embeddedOfferDifferingOnlyInTradeProtocolVersionIsAccepted() throws GeneralSecurityException {
        // tradeProtocolVersion is excluded from both equals and the hash (@ExcludeForHash), so a
        // peer whose copy carries a different version must still pass the hash-relevant comparison.
        MuSigOffer makersOffer = createOffer(new BaseSideFixedAmountSpec(2_000_000L));
        bisq.offer.protobuf.Offer otherVersionProto = makersOffer.toProto(false).toBuilder()
                .setTradeProtocolVersion("9.9.9")
                .build();
        MuSigOffer otherVersion = MuSigOffer.fromProto(otherVersionProto);
        MuSigContract contract = createContract(otherVersion, 2_000_000L, 10_000_000L,
                otherVersion.getPriceSpec(), Optional.empty(), Optional.empty());
        SetupTradeMessage_A message = createMessage(contract, TAKER_NETWORK_ID, sign(contract, TAKER_KEY_PAIR));
        MyMuSigOffersService myOffers = org.mockito.Mockito.mock(MyMuSigOffersService.class);
        org.mockito.Mockito.when(myOffers.findActivatedOffer("test-id")).thenReturn(Optional.of(makersOffer));

        assertThatCode(() -> MuSigTakeOfferRequestValidator.validateOffer(myOffers, message))
                .doesNotThrowAnyException();
    }

    @Test
    void unknownTakerProfileIsRejected() throws GeneralSecurityException {
        MuSigContract contract = createContract();
        SetupTradeMessage_A message = createMessage(contract, TAKER_NETWORK_ID, sign(contract, TAKER_KEY_PAIR));
        bisq.user.profile.UserProfileService userProfileService =
                org.mockito.Mockito.mock(bisq.user.profile.UserProfileService.class);
        org.mockito.Mockito.when(userProfileService.findUserProfile(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> MuSigTakeOfferRequestValidator.validateTakerProfileKnown(userProfileService, message))
                .isInstanceOf(TradeProtocolException.class);
    }

    @Test
    void missingAccountPayloadCommitmentIsRejected() {
        // An ABSENT commitment survives deserialization because Party.verify validates the
        // hash only when present; the case is built through the actual wire path by clearing
        // the field from a valid contract's proto. Dev mode isolates the commitment check from
        // the dispute-agent presence check, which would otherwise also reject this contract.
        MuSigOffer offer = createOffer(new BaseSideFixedAmountSpec(2_000_000L));
        MuSigContract valid = createContract(offer, 2_000_000L, 10_000_000L, offer.getPriceSpec(),
                Optional.empty(), Optional.empty());
        bisq.contract.protobuf.Contract proto = valid.toProto(true);
        bisq.contract.protobuf.Contract mutated = proto.toBuilder()
                .setTwoPartyContract(proto.getTwoPartyContract().toBuilder()
                        .setTaker(proto.getTwoPartyContract().getTaker().toBuilder()
                                .clearSaltedAccountPayloadHash()))
                .build();
        MuSigContract contract = MuSigContract.fromProto(mutated);

        bisq.common.application.DevMode.setDevMode(true);
        try {
            assertEconomicsRejected(contract, TradeProtocolFailure.OFFER_NOT_AVAILABLE);
        } finally {
            bisq.common.application.DevMode.setDevMode(false);
        }
    }

    /* ------------------------------------------------------------------ */
    // Economics
    /* ------------------------------------------------------------------ */

    @Test
    void consistentEconomicsPass() {
        // 0.02 BTC at $50,000 fixed: quote 1,000 USD; base pinned by the fixed spec.
        MuSigContract contract = fiatContract(2_000_000L, 10_000_000L);
        assertEconomicsAccepted(contract);
    }

    @Test
    void nonPositiveAmountsAreRejected() {
        assertEconomicsRejected(fiatContract(2_000_000L, -1L), TradeProtocolFailure.OFFER_NOT_AVAILABLE);
        assertEconomicsRejected(fiatContract(0L, 10_000_000L), TradeProtocolFailure.OFFER_NOT_AVAILABLE);
    }

    @Test
    void priceSpecDifferingFromOfferIsRejected() {
        MuSigOffer offer = createOffer(new BaseSideFixedAmountSpec(2_000_000L));
        MuSigContract contract = createContract(offer, 2_000_000L, 10_000_000L,
                new FixPriceSpec(PriceQuote.fromFiatPrice(45_000, "USD")),
                Optional.of(mediatorProfile), Optional.of(arbitratorProfile));
        assertEconomicsRejected(contract, TradeProtocolFailure.OFFER_NOT_AVAILABLE);
    }

    @Test
    void baseAmountOffTheFixedSpecIsRejected() {
        // The base side is pinned exactly by the offer's fixed amount spec.
        assertEconomicsRejected(fiatContract(1_900_000L, 10_000_000L), TradeProtocolFailure.OFFER_NOT_AVAILABLE);
    }

    @Test
    void wholeFiatRoundingAtBaseRangeBoundsIsAccepted() {
        PriceQuote price = PriceQuote.fromFiatPrice(50_131, "USD");
        MuSigOffer offer = createOffer(new BaseSideRangeAmountSpec(1_000_000L, 2_000_000L), price);

        long roundedMinQuote = price.toQuoteSideMonetary(Coin.asBtcFromValue(1_000_000L)).round(0).getValue();
        long derivedMinBase = price.toBaseSideMonetary(Fiat.fromValue(roundedMinQuote, "USD")).getValue();
        long roundedMaxQuote = price.toQuoteSideMonetary(Coin.asBtcFromValue(2_000_000L)).round(0).getValue();
        long derivedMaxBase = price.toBaseSideMonetary(Fiat.fromValue(roundedMaxQuote, "USD")).getValue();

        assertThat(derivedMinBase).isEqualTo(999_382L);
        assertThat(derivedMaxBase).isEqualTo(2_000_758L);
        assertEconomicsAccepted(createContract(offer, derivedMinBase, roundedMinQuote, offer.getPriceSpec(),
                Optional.of(mediatorProfile), Optional.of(arbitratorProfile)));
        assertEconomicsAccepted(createContract(offer, derivedMaxBase, roundedMaxQuote, offer.getPriceSpec(),
                Optional.of(mediatorProfile), Optional.of(arbitratorProfile)));
    }

    @Test
    void wholeFiatRoundingAtBaseFixedAmountIsAccepted() {
        PriceQuote price = PriceQuote.fromFiatPrice(50_131, "USD");
        MuSigOffer offer = createOffer(new BaseSideFixedAmountSpec(2_000_000L), price);
        long roundedQuote = price.toQuoteSideMonetary(Coin.asBtcFromValue(2_000_000L)).round(0).getValue();
        long derivedBase = price.toBaseSideMonetary(Fiat.fromValue(roundedQuote, "USD")).getValue();

        assertThat(derivedBase).isEqualTo(2_000_758L);
        assertEconomicsAccepted(createContract(offer, derivedBase, roundedQuote, offer.getPriceSpec(),
                Optional.of(mediatorProfile), Optional.of(arbitratorProfile)));
    }

    @Test
    void baseRangeAmountBeyondWholeFiatRoundingIsRejected() {
        PriceQuote price = PriceQuote.fromFiatPrice(50_131, "USD");
        MuSigOffer offer = createOffer(new BaseSideRangeAmountSpec(1_000_000L, 2_000_000L), price);
        long outsideBase = 2_002_000L;
        long roundedQuote = price.toQuoteSideMonetary(Coin.asBtcFromValue(outsideBase)).round(0).getValue();
        MuSigContract contract = createContract(offer, outsideBase, roundedQuote, offer.getPriceSpec(),
                Optional.of(mediatorProfile), Optional.of(arbitratorProfile));

        assertEconomicsRejected(contract, TradeProtocolFailure.OFFER_NOT_AVAILABLE);
    }

    @Test
    void baseAmountOutsideSpecWithoutWholeFiatQuoteIsRejected() {
        PriceQuote price = PriceQuote.fromFiatPrice(50_131, "USD");
        MuSigOffer offer = createOffer(new BaseSideFixedAmountSpec(2_000_000L), price);
        long outsideBase = 2_000_001L;
        long unroundedQuote = price.toQuoteSideMonetary(Coin.asBtcFromValue(outsideBase)).getValue();
        MuSigContract contract = createContract(offer, outsideBase, unroundedQuote, offer.getPriceSpec(),
                Optional.of(mediatorProfile), Optional.of(arbitratorProfile));

        assertThat(unroundedQuote % Fiat.fromFaceValue(1, "USD").getValue()).isNotZero();
        assertEconomicsRejected(contract, TradeProtocolFailure.OFFER_NOT_AVAILABLE);
    }

    @Test
    void quoteAmountBeyondPriceToleranceIsRejected() {
        // Maker gives the quote side (maker buys BTC): a quote 10% above the resolved price is
        // adverse and beyond the 5% tolerance; 4% above is tolerated.
        assertEconomicsRejected(fiatContract(2_000_000L, 11_000_000L), TradeProtocolFailure.PRICE_DEVIATION);
        assertEconomicsAccepted(fiatContract(2_000_000L, 10_400_000L));
    }

    @Test
    void altcoinBuyOfferRejectsTooMuchBitcoinGiven() {
        // Offer.direction is the raw BASE-currency direction. XMR/BTC raw BUY means the maker buys
        // XMR (the base) and GIVES BTC (the quote), so giving too much BTC is adverse. 4 XMR at
        // 0.005 BTC/XMR: expected quote 2,000,000 sats, 5% tolerance -> upper bound 2,100,000.
        assertEconomicsRejected(altcoinContract(Direction.BUY, 4_000_000_000_000L, 2_300_000L),
                TradeProtocolFailure.PRICE_DEVIATION);
        assertEconomicsAccepted(altcoinContract(Direction.BUY, 4_000_000_000_000L, 2_050_000L));
    }

    @Test
    void altcoinSellOfferRejectsTooLittleBitcoinReceived() {
        // XMR/BTC raw SELL means the maker sells XMR and RECEIVES BTC, so receiving too little BTC
        // is adverse - the mirror bound of the BUY case. Expected quote 2,000,000 sats, 5%
        // tolerance -> lower bound 1,900,000.
        assertEconomicsRejected(altcoinContract(Direction.SELL, 4_000_000_000_000L, 1_700_000L),
                TradeProtocolFailure.PRICE_DEVIATION);
        assertEconomicsAccepted(altcoinContract(Direction.SELL, 4_000_000_000_000L, 1_950_000L));
    }

    @Test
    void paymentSpecNotOfferedIsRejected() {
        MuSigOffer offer = createOffer(new BaseSideFixedAmountSpec(2_000_000L));
        PaymentMethodSpec<?> foreignSpec = PaymentMethodSpecUtil.createPaymentMethodSpec(
                FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.WISE), "USD");
        MuSigContract contract = new MuSigContract(offer.getDate(),
                offer,
                TAKER_NETWORK_ID,
                2_000_000L,
                10_000_000L,
                foreignSpec,
                new byte[20],
                Optional.of(mediatorProfile),
                Optional.of(arbitratorProfile),
                offer.getPriceSpec(),
                0);
        assertEconomicsRejected(contract, TradeProtocolFailure.OFFER_NOT_AVAILABLE);
    }

    @Test
    void overSupplyBitcoinAmountIsRejected() {
        // A Bitcoin-side amount above the total supply is rejected before the USD conversion, even
        // when the quote is consistent with the price (so the two-sided tolerance passes first).
        long baseSats = 2_100_000_000_000_001L; // MAX_BITCOIN_SUPPLY_SATS + 1
        PriceQuote price = PriceQuote.fromFiatPrice(50_000, "USD");
        long consistentQuote = price.toQuoteSideMonetary(Coin.asBtcFromValue(baseSats)).getValue();
        Market market = new Market("BTC", "USD", "Bitcoin", "US Dollar");
        PaymentMethodSpec<?> paymentMethodSpec = PaymentMethodSpecUtil.createPaymentMethodSpec(
                FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ACH_TRANSFER), "USD");
        MuSigOffer offer = new MuSigOffer("test-id", OFFER_MAKER_NETWORK_ID, Direction.BUY, market,
                new BaseSideFixedAmountSpec(baseSats), new FixPriceSpec(price),
                List.of(paymentMethodSpec.getPaymentMethod()), List.of(), "1.0.0");
        MuSigContract contract = createContract(offer, baseSats, consistentQuote, offer.getPriceSpec(),
                Optional.of(mediatorProfile), Optional.of(arbitratorProfile));

        assertEconomicsRejected(contract, TradeProtocolFailure.OFFER_NOT_AVAILABLE);
    }

    @Test
    void favorableFiatAmountBeyondToleranceIsRejected() {
        // A one-sided tolerance would let an unboundedly favorable side through, defeating the rail
        // cap: a BTC/USD SELL maker receives the USD side, so a taker could inflate the quote far
        // above the Bitcoin value; the rail check - valued on the 0.1 BTC = $5,000 - passes its
        // $5,000 ACH cap, and the maker would co-sign a $100,000 ACH contract. Two-sided tolerance
        // rejects the inflated quote (20x, far beyond the 5% band).
        Market market = new Market("BTC", "USD", "Bitcoin", "US Dollar");
        PaymentMethodSpec<?> paymentMethodSpec = PaymentMethodSpecUtil.createPaymentMethodSpec(
                FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ACH_TRANSFER), "USD");
        MuSigOffer offer = new MuSigOffer("test-id", OFFER_MAKER_NETWORK_ID, Direction.SELL, market,
                new BaseSideFixedAmountSpec(10_000_000L), new FixPriceSpec(PriceQuote.fromFiatPrice(50_000, "USD")),
                List.of(paymentMethodSpec.getPaymentMethod()), List.of(), "1.0.0");
        // base 0.1 BTC, quote 1,000,000,000 = $100,000 (expected $5,000).
        MuSigContract contract = createContract(offer, 10_000_000L, 1_000_000_000L, offer.getPriceSpec(),
                Optional.of(mediatorProfile), Optional.of(arbitratorProfile));

        assertEconomicsRejected(contract, TradeProtocolFailure.PRICE_DEVIATION);
    }

    @Test
    void takeDateEarlierThanTheClockSkewWindowIsRejected() {
        MuSigOffer offer = createOffer(new BaseSideFixedAmountSpec(2_000_000L));
        MuSigContract contract = new MuSigContract(offer.getDate() - TWO_HOURS - 1,
                offer,
                TAKER_NETWORK_ID,
                2_000_000L,
                10_000_000L,
                offer.getQuoteSidePaymentMethodSpecs().get(0),
                new byte[20],
                Optional.of(mediatorProfile),
                Optional.of(arbitratorProfile),
                offer.getPriceSpec(),
                0);

        assertEconomicsRejected(contract, TradeProtocolFailure.OFFER_NOT_AVAILABLE);
    }

    @Test
    void takeDateWithinTheClockSkewWindowIsAccepted() {
        MuSigOffer offer = createOffer(new BaseSideFixedAmountSpec(2_000_000L));
        MuSigContract contract = new MuSigContract(offer.getDate() - TWO_HOURS + 1,
                offer,
                TAKER_NETWORK_ID,
                2_000_000L,
                10_000_000L,
                offer.getQuoteSidePaymentMethodSpecs().get(0),
                new byte[20],
                Optional.of(mediatorProfile),
                Optional.of(arbitratorProfile),
                offer.getPriceSpec(),
                0);

        assertEconomicsAccepted(contract);
    }

    @Test
    void staleBtcUsdPriceIsRejected() {
        // A fixed-price offer passes the tolerance check, but the absolute USD limits are valued
        // with the BTC/USD market price. A >12h-stale quote (kept after a failed refresh) must be
        // rejected rather than trusted.
        bisq.bonded_roles.market_price.MarketPrice stalePrice =
                org.mockito.Mockito.mock(bisq.bonded_roles.market_price.MarketPrice.class);
        org.mockito.Mockito.when(stalePrice.isValidDate()).thenReturn(false);
        org.mockito.Mockito.when(stalePrice.getPriceQuote()).thenReturn(PriceQuote.fromFiatPrice(50_000, "USD"));
        org.mockito.Mockito.when(marketPriceService.findMarketPrice(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Optional.of(stalePrice));

        assertEconomicsRejected(fiatContract(2_000_000L, 10_000_000L), TradeProtocolFailure.OFFER_NOT_AVAILABLE);
    }

    @Test
    void staleMarketPriceOfferIsRejected() {
        // A market-price offer resolves against the live market price; a stale quote for its own
        // market must be rejected before the amounts are validated against an outdated price.
        bisq.bonded_roles.market_price.MarketPrice stalePrice =
                org.mockito.Mockito.mock(bisq.bonded_roles.market_price.MarketPrice.class);
        org.mockito.Mockito.when(stalePrice.isValidDate()).thenReturn(false);
        org.mockito.Mockito.when(stalePrice.getPriceQuote()).thenReturn(PriceQuote.fromFiatPrice(50_000, "USD"));
        org.mockito.Mockito.when(marketPriceService.findMarketPrice(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Optional.of(stalePrice));
        Market market = new Market("BTC", "USD", "Bitcoin", "US Dollar");
        PaymentMethodSpec<?> nonBtcPaymentMethodSpec = PaymentMethodSpecUtil.createPaymentMethodSpec(
                FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ACH_TRANSFER), "USD");
        MuSigOffer offer = new MuSigOffer("test-id",
                OFFER_MAKER_NETWORK_ID,
                Direction.SELL,
                market,
                new BaseSideFixedAmountSpec(2_000_000L),
                new bisq.offer.price.spec.MarketPriceSpec(),
                List.of(nonBtcPaymentMethodSpec.getPaymentMethod()),
                List.of(),
                "1.0.0");
        MuSigContract contract = createContract(offer, 2_000_000L, 10_000_000L, offer.getPriceSpec(),
                Optional.of(mediatorProfile), Optional.of(arbitratorProfile));

        assertEconomicsRejected(contract, TradeProtocolFailure.OFFER_NOT_AVAILABLE);
    }

    @Test
    void zeroValuedResolvedPriceIsRejected() {
        // Nothing upstream rejects a zero price. For a market-price offer whose fresh quote is
        // zero, the cross-multiplication denominator would be zero and the tolerance check would
        // fail open, accepting any amount. The BTC/USD price stays fresh and non-zero so the USD
        // limits do not reject first - only the non-positive guard catches it.
        Market xmrBtc = new Market("XMR", "BTC", "Monero", "Bitcoin");
        bisq.bonded_roles.market_price.MarketPrice zeroPrice =
                org.mockito.Mockito.mock(bisq.bonded_roles.market_price.MarketPrice.class);
        org.mockito.Mockito.when(zeroPrice.isValidDate()).thenReturn(true);
        org.mockito.Mockito.when(zeroPrice.getPriceQuote()).thenReturn(PriceQuote.fromPrice(0L, "XMR", "BTC"));
        org.mockito.Mockito.when(marketPriceService.findMarketPrice(xmrBtc)).thenReturn(Optional.of(zeroPrice));
        PaymentMethodSpec<?> cryptoSpec = PaymentMethodSpecUtil.createPaymentMethodSpec(
                bisq.account.payment_method.crypto.CryptoPaymentMethod.fromCustomName("XMR", "XMR"), "XMR");
        MuSigOffer offer = new MuSigOffer("test-id",
                OFFER_MAKER_NETWORK_ID,
                Direction.SELL,
                xmrBtc,
                new bisq.offer.amount.spec.QuoteSideFixedAmountSpec(100_000L),
                new bisq.offer.price.spec.MarketPriceSpec(),
                List.of(cryptoSpec.getPaymentMethod()),
                List.of(),
                "1.0.0");
        MuSigContract contract = createContract(offer, 5_000_000_000_000L, 100_000L, offer.getPriceSpec(),
                Optional.of(mediatorProfile), Optional.of(arbitratorProfile));

        assertEconomicsRejected(contract, TradeProtocolFailure.OFFER_NOT_AVAILABLE);
    }

    @Test
    void marketPriceOfferReadsASingleFreshnessSnapshot() {
        // The freshness check and the validated quote must come from ONE MarketPrice lookup - a
        // second lookup could pass freshness on a different object than the quote came from (the
        // map is replaced concurrently). Verify the market-based path reads the offer market's
        // price exactly once.
        Market xmrBtc = new Market("XMR", "BTC", "Monero", "Bitcoin");
        bisq.bonded_roles.market_price.MarketPrice freshXmrPrice =
                org.mockito.Mockito.mock(bisq.bonded_roles.market_price.MarketPrice.class);
        org.mockito.Mockito.when(freshXmrPrice.isValidDate()).thenReturn(true);
        org.mockito.Mockito.when(freshXmrPrice.getPriceQuote()).thenReturn(PriceQuote.fromPrice(0.005, "XMR", "BTC"));
        org.mockito.Mockito.when(marketPriceService.findMarketPrice(xmrBtc)).thenReturn(Optional.of(freshXmrPrice));
        PaymentMethodSpec<?> cryptoSpec = PaymentMethodSpecUtil.createPaymentMethodSpec(
                bisq.account.payment_method.crypto.CryptoPaymentMethod.fromCustomName("XMR", "XMR"), "XMR");
        MuSigOffer offer = new MuSigOffer("test-id",
                OFFER_MAKER_NETWORK_ID,
                Direction.SELL,
                xmrBtc,
                new BaseSideFixedAmountSpec(4_000_000_000_000L),
                new bisq.offer.price.spec.MarketPriceSpec(),
                List.of(cryptoSpec.getPaymentMethod()),
                List.of(),
                "1.0.0");
        // 4 XMR at 0.005 BTC/XMR = 2,000,000 sats, exactly the expected quote.
        MuSigContract contract = createContract(offer, 4_000_000_000_000L, 2_000_000L, offer.getPriceSpec(),
                Optional.of(mediatorProfile), Optional.of(arbitratorProfile));
        assertEconomicsAccepted(contract);

        org.mockito.Mockito.verify(marketPriceService, org.mockito.Mockito.times(1)).findMarketPrice(xmrBtc);
    }

    @Test
    void quotePinnedAmountThatWouldWrapThePriceConversionIsRejected() {
        // XMR/BTC BUY offer (the maker buys XMR and receives it, so too little XMR is adverse),
        // quote (BTC) side pinned at 0.1 BTC (10,000,000 sats). At an extreme resolved price of
        // 1 sat/XMR the expected base amount is 10^7 * 10^12 = 10^19, which overflows a long and
        // wraps negative in PriceQuote.toBaseSideMonetary. The wrapped value makes the tolerance
        // comparison "1 < negative" pass a 1-piconero base against a 10^19 expectation; the exact
        // BigDecimal comparison rejects it.
        Market market = new Market("XMR", "BTC", "Monero", "Bitcoin");
        PaymentMethodSpec<?> cryptoSpec = PaymentMethodSpecUtil.createPaymentMethodSpec(
                bisq.account.payment_method.crypto.CryptoPaymentMethod.fromCustomName("XMR", "XMR"), "XMR");
        MuSigOffer offer = new MuSigOffer("test-id",
                OFFER_MAKER_NETWORK_ID,
                Direction.BUY,
                market,
                new bisq.offer.amount.spec.QuoteSideFixedAmountSpec(10_000_000L),
                new FixPriceSpec(PriceQuote.fromPrice(1L, "XMR", "BTC")),
                List.of(cryptoSpec.getPaymentMethod()),
                List.of(),
                "1.0.0");
        MuSigContract contract = createContract(offer, 1L, 10_000_000L, offer.getPriceSpec(),
                Optional.of(mediatorProfile), Optional.of(arbitratorProfile));

        assertEconomicsRejected(contract, TradeProtocolFailure.PRICE_DEVIATION);
    }

    @Test
    void quotePinnedAmountJustOverTheExactToleranceIsRejected() {
        // XMR/BTC SELL (the maker sells XMR and gives it, so too much XMR is adverse), quote (BTC)
        // side pinned at 1,000,000 sats, price 7 sats/XMR, 5% tolerance. Exact expected base =
        // 1,000,000 * 10^12 / 7 piconero; the exact 5% upper bound is exactly 150,000,000,000,000,000
        // piconero. A base 20 piconero above it must be rejected - a rounded (DECIMAL64) bound would
        // widen to 150,000,000,000,000,045 and wrongly accept it.
        Market market = new Market("XMR", "BTC", "Monero", "Bitcoin");
        PaymentMethodSpec<?> cryptoSpec = PaymentMethodSpecUtil.createPaymentMethodSpec(
                bisq.account.payment_method.crypto.CryptoPaymentMethod.fromCustomName("XMR", "XMR"), "XMR");
        MuSigOffer offer = new MuSigOffer("test-id",
                OFFER_MAKER_NETWORK_ID,
                Direction.SELL,
                market,
                new bisq.offer.amount.spec.QuoteSideFixedAmountSpec(1_000_000L),
                new FixPriceSpec(PriceQuote.fromPrice(7L, "XMR", "BTC")),
                List.of(cryptoSpec.getPaymentMethod()),
                List.of(),
                "1.0.0");
        MuSigContract overLimit = createContract(offer, 150_000_000_000_000_020L, 1_000_000L,
                offer.getPriceSpec(), Optional.of(mediatorProfile), Optional.of(arbitratorProfile));
        assertEconomicsRejected(overLimit, TradeProtocolFailure.PRICE_DEVIATION);

        // Exactly at the bound is within tolerance and accepted.
        MuSigContract atLimit = createContract(offer, 150_000_000_000_000_000L, 1_000_000L,
                offer.getPriceSpec(), Optional.of(mediatorProfile), Optional.of(arbitratorProfile));
        assertEconomicsAccepted(atLimit);
    }

    @Test
    void amountBeyondTheRailLimitIsRejected() {
        // 0.18 BTC at $50,000 is $9,000; ACH transfer's chargeback tier caps at $5,000.
        assertEconomicsRejected(fiatContract(18_000_000L, 90_000_000L, new BaseSideFixedAmountSpec(18_000_000L)),
                TradeProtocolFailure.OFFER_NOT_AVAILABLE);
    }

    @Test
    void amountBelowTheAbsoluteMinimumIsRejected() {
        // 0.0001 BTC at $50,000 is $5, below the $10 absolute minimum.
        assertEconomicsRejected(fiatContract(10_000L, 50_000L, new BaseSideFixedAmountSpec(10_000L)),
                TradeProtocolFailure.OFFER_NOT_AVAILABLE);
    }

    @Test
    void mediatorNotMatchingOwnSelectionIsRejected() {
        MuSigContract contract = fiatContract(2_000_000L, 10_000_000L);
        UserProfile myMediator = org.mockito.Mockito.mock(UserProfile.class);
        org.mockito.Mockito.when(myMediator.getNetworkId()).thenReturn(OTHER_NETWORK_ID);
        org.mockito.Mockito.when(myMediator.getUserName()).thenReturn("other");
        org.mockito.Mockito.when(mediationService.selectMediator(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(myMediator));

        assertEconomicsRejected(contract, TradeProtocolFailure.MEDIATORS_NOT_MATCHING);
    }

    @Test
    void mediatorWithForgedNicknameIsRejected() {
        bisq.security.pow.ProofOfWork proofOfWork = org.mockito.Mockito.mock(bisq.security.pow.ProofOfWork.class);
        org.mockito.Mockito.when(proofOfWork.getSolution()).thenReturn(new byte[72]);
        NetworkId agentNetworkId = createNetworkId("dispute-agent", 9988, null);
        UserProfile genuine = new UserProfile(0, "agent", proofOfWork, 0, agentNetworkId, "", "", "");
        UserProfile forged = new UserProfile(0, "forged", proofOfWork, 0, agentNetworkId, "", "", "");
        org.mockito.Mockito.when(mediationService.selectMediator(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(genuine));
        MuSigOffer offer = createOffer(new BaseSideFixedAmountSpec(2_000_000L));
        MuSigContract contract = createContract(offer, 2_000_000L, 10_000_000L, offer.getPriceSpec(),
                Optional.of(forged), Optional.of(arbitratorProfile));

        assertEconomicsRejected(contract, TradeProtocolFailure.MEDIATORS_NOT_MATCHING);
    }

    @Test
    void mediatorWithForgedAvatarVersionIsRejected() {
        // Two profiles equal in every field UserProfile.equals covers but differing in
        // avatarVersion, which equals ignores; an unsupported value breaks avatar rendering, so
        // the maker's selection and the contract's copy must also agree on it. Real profiles are
        // used (a shared proof of work) so equals actually returns true and the check falls
        // through to the avatarVersion comparison.
        bisq.security.pow.ProofOfWork proofOfWork = org.mockito.Mockito.mock(bisq.security.pow.ProofOfWork.class);
        org.mockito.Mockito.when(proofOfWork.getSolution()).thenReturn(new byte[72]);
        NetworkId agentNetworkId = createNetworkId("dispute-agent", 9988, null);
        UserProfile genuine = new UserProfile(0, "agent", proofOfWork, 0, agentNetworkId, "", "", "");
        UserProfile forged = new UserProfile(0, "agent", proofOfWork, Integer.MAX_VALUE, agentNetworkId, "", "", "");
        org.mockito.Mockito.when(mediationService.selectMediator(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(genuine));
        MuSigOffer offer = createOffer(new BaseSideFixedAmountSpec(2_000_000L));
        MuSigContract contract = createContract(offer, 2_000_000L, 10_000_000L, offer.getPriceSpec(),
                Optional.of(forged), Optional.of(arbitratorProfile));

        assertEconomicsRejected(contract, TradeProtocolFailure.MEDIATORS_NOT_MATCHING);
    }

    @Test
    void mediatorWithForgedApplicationVersionIsRejected() {
        // applicationVersion is excluded from UserProfile.equals but is hash-relevant for a
        // version-1 profile, so the maker's selection and the contract's copy must agree on it,
        // else a taker forges a different agent profile into the co-signed contract.
        bisq.security.pow.ProofOfWork proofOfWork = org.mockito.Mockito.mock(bisq.security.pow.ProofOfWork.class);
        org.mockito.Mockito.when(proofOfWork.getSolution()).thenReturn(new byte[72]);
        NetworkId agentNetworkId = createNetworkId("dispute-agent", 9988, null);
        UserProfile genuine = new UserProfile(1, "agent", proofOfWork, 0, agentNetworkId, "", "", "2.1.4");
        UserProfile forged = new UserProfile(1, "agent", proofOfWork, 0, agentNetworkId, "", "", "9.9.9");
        org.mockito.Mockito.when(mediationService.selectMediator(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(genuine));
        MuSigOffer offer = createOffer(new BaseSideFixedAmountSpec(2_000_000L));
        MuSigContract contract = createContract(offer, 2_000_000L, 10_000_000L, offer.getPriceSpec(),
                Optional.of(forged), Optional.of(arbitratorProfile));

        assertEconomicsRejected(contract, TradeProtocolFailure.MEDIATORS_NOT_MATCHING);
    }

    @Test
    void fiatObligationExceedingTheRailCapIsRejected() {
        // The two-sided tolerance lets the USD quote sit up to 5% above the Bitcoin value, so the
        // rail cap must be valued on the actual fiat obligation, not the Bitcoin side. 0.1 BTC at
        // $50,000 is exactly the $5,000 ACH cap on the Bitcoin side, but a +5% quote is a $5,250
        // ACH obligation above the cap.
        assertEconomicsRejected(fiatContract(10_000_000L, 52_500_000L, new BaseSideFixedAmountSpec(10_000_000L)),
                TradeProtocolFailure.OFFER_NOT_AVAILABLE);
        // The consistent $5,000 quote is at the cap and accepted.
        assertEconomicsAccepted(fiatContract(10_000_000L, 50_000_000L, new BaseSideFixedAmountSpec(10_000_000L)));
    }

    @Test
    void arbitratorNotMatchingOwnSelectionIsRejected() {
        MuSigContract contract = fiatContract(2_000_000L, 10_000_000L);
        UserProfile myArbitrator = org.mockito.Mockito.mock(UserProfile.class);
        org.mockito.Mockito.when(myArbitrator.getNetworkId()).thenReturn(OTHER_NETWORK_ID);
        org.mockito.Mockito.when(myArbitrator.getUserName()).thenReturn("other");
        org.mockito.Mockito.when(arbitrationService.selectArbitrator(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn(Optional.of(myArbitrator));

        assertEconomicsRejected(contract, TradeProtocolFailure.MEDIATORS_NOT_MATCHING);
    }

    @Test
    void missingDisputeAgentsAreRejectedOutsideDevMode() {
        MuSigOffer offer = createOffer(new BaseSideFixedAmountSpec(2_000_000L));
        MuSigContract contract = createContract(offer, 2_000_000L, 10_000_000L, offer.getPriceSpec(),
                Optional.empty(), Optional.empty());
        org.mockito.Mockito.when(mediationService.selectMediator(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.empty());
        org.mockito.Mockito.when(arbitrationService.selectArbitrator(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn(Optional.empty());

        // The selections agree with the contract, but a production trade without dispute
        // agents would leave mediation and arbitration without a recipient.
        assertEconomicsRejected(contract, TradeProtocolFailure.OFFER_NOT_AVAILABLE);
    }

    private MuSigContract fiatContract(long baseSideAmount, long quoteSideAmount) {
        return fiatContract(baseSideAmount, quoteSideAmount, new BaseSideFixedAmountSpec(2_000_000L));
    }

    private MuSigContract fiatContract(long baseSideAmount, long quoteSideAmount, bisq.offer.amount.spec.AmountSpec amountSpec) {
        MuSigOffer offer = createOffer(amountSpec);
        return createContract(offer, baseSideAmount, quoteSideAmount, offer.getPriceSpec(),
                Optional.of(mediatorProfile), Optional.of(arbitratorProfile));
    }

    private MuSigContract altcoinContract(Direction direction, long baseSideAmount, long quoteSideAmount) {
        Market market = new Market("XMR", "BTC", "Monero", "Bitcoin");
        PaymentMethodSpec<?> cryptoSpec = PaymentMethodSpecUtil.createPaymentMethodSpec(
                bisq.account.payment_method.crypto.CryptoPaymentMethod.fromCustomName("XMR", "XMR"), "XMR");
        MuSigOffer offer = new MuSigOffer("test-id",
                OFFER_MAKER_NETWORK_ID,
                direction,
                market,
                new BaseSideFixedAmountSpec(baseSideAmount),
                new FixPriceSpec(PriceQuote.fromPrice(0.005, "XMR", "BTC")),
                List.of(cryptoSpec.getPaymentMethod()),
                List.of(),
                "1.0.0");
        return createContract(offer, baseSideAmount, quoteSideAmount, offer.getPriceSpec(),
                Optional.of(mediatorProfile), Optional.of(arbitratorProfile));
    }

    private MuSigContract createContract(MuSigOffer offer,
                                         long baseSideAmount,
                                         long quoteSideAmount,
                                         bisq.offer.price.spec.PriceSpec priceSpec,
                                         Optional<UserProfile> mediator,
                                         Optional<UserProfile> arbitrator) {
        PaymentMethodSpec<?> paymentMethodSpec = offer.getMarket().isBaseCurrencyBitcoin()
                ? offer.getQuoteSidePaymentMethodSpecs().get(0)
                : offer.getBaseSidePaymentMethodSpecs().get(0);
        return new MuSigContract(offer.getDate(),
                offer,
                TAKER_NETWORK_ID,
                baseSideAmount,
                quoteSideAmount,
                paymentMethodSpec,
                new byte[20],
                mediator,
                arbitrator,
                priceSpec,
                0);
    }

    private void assertEconomicsAccepted(MuSigContract contract) {
        assertThatCode(() -> MuSigTakeOfferRequestValidator.validateEconomics(marketPriceService,
                settingsService, mediationService, arbitrationService, economicsMessage(contract)))
                .doesNotThrowAnyException();
    }

    private void assertEconomicsRejected(MuSigContract contract, TradeProtocolFailure expectedFailure) {
        assertThatThrownBy(() -> MuSigTakeOfferRequestValidator.validateEconomics(marketPriceService,
                settingsService, mediationService, arbitrationService, economicsMessage(contract)))
                .isInstanceOf(TradeProtocolException.class)
                .satisfies(e -> org.assertj.core.api.Assertions.assertThat(
                                ((TradeProtocolException) e).getTradeProtocolFailure())
                        .isEqualTo(expectedFailure));
    }

    private SetupTradeMessage_A economicsMessage(MuSigContract contract) {
        try {
            // The economics checks never read the signature; a donor signature from the plain
            // fixture contract avoids serializing the mocked dispute-agent profiles.
            return createMessage(contract, TAKER_NETWORK_ID, sign(createContract(), TAKER_KEY_PAIR));
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    private void assertOfferRejected(MyMuSigOffersService myOffers, SetupTradeMessage_A message) {
        assertThatThrownBy(() -> MuSigTakeOfferRequestValidator.validateOffer(myOffers, message))
                .isInstanceOf(TradeProtocolException.class)
                .satisfies(e -> org.assertj.core.api.Assertions.assertThat(
                                ((TradeProtocolException) e).getTradeProtocolFailure())
                        .isEqualTo(TradeProtocolFailure.OFFER_NOT_AVAILABLE));
    }

    private void assertRejected(SetupTradeMessage_A message) {
        assertThatThrownBy(() -> MuSigTakeOfferRequestValidator.validateIdentity(contractService, message))
                .isInstanceOf(TradeProtocolException.class)
                .satisfies(e -> org.assertj.core.api.Assertions.assertThat(
                                ((TradeProtocolException) e).getTradeProtocolFailure())
                        .isEqualTo(TradeProtocolFailure.OFFER_NOT_AVAILABLE));
    }

    private ContractSignatureData sign(MuSigContract contract, KeyPair keyPair) throws GeneralSecurityException {
        return contractService.signContract(contract, keyPair);
    }

    private static SetupTradeMessage_A createMessage(MuSigContract contract,
                                                     NetworkId sender,
                                                     ContractSignatureData contractSignatureData) {
        return createMessage(contract, sender, OFFER_MAKER_NETWORK_ID, contractSignatureData,
                Trade.createId(contract.getOffer().getId(),
                        contract.getTaker().getNetworkId().getId(),
                        contract.getTakeOfferDate()));
    }

    private static SetupTradeMessage_A createMessage(MuSigContract contract,
                                                     NetworkId sender,
                                                     NetworkId receiver,
                                                     ContractSignatureData contractSignatureData,
                                                     String tradeId) {
        return new SetupTradeMessage_A("message-id",
                tradeId,
                "1.0.0",
                sender,
                receiver,
                contract,
                contractSignatureData,
                PubKeyShares.fromProto(bisq.trade.protobuf.PubKeyShares.newBuilder()
                        .setBuyerOutputPubKeyShare(com.google.protobuf.ByteString.copyFrom(compressedKey()))
                        .setSellerOutputPubKeyShare(com.google.protobuf.ByteString.copyFrom(compressedKey()))
                        .setMultisigScriptKey(com.google.protobuf.ByteString.copyFrom(compressedKey()))
                        .build()));
    }

    private static byte[] compressedKey() {
        byte[] key = new byte[33];
        key[0] = 0x02;
        return key;
    }

    private static MuSigContract createContract() {
        Market market = new Market("BTC", "USD", "Bitcoin", "US Dollar");
        PaymentMethodSpec<?> nonBtcPaymentMethodSpec = PaymentMethodSpecUtil.createPaymentMethodSpec(
                FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ACH_TRANSFER),
                "USD");
        MuSigOffer offer = createOffer(new BaseSideFixedAmountSpec(111L));
        return new MuSigContract(offer.getDate(),
                offer,
                TAKER_NETWORK_ID,
                111L,
                222L,
                nonBtcPaymentMethodSpec,
                new byte[20],
                Optional.empty(),
                Optional.empty(),
                offer.getPriceSpec(),
                0);
    }

    private static MuSigOffer createOffer(bisq.offer.amount.spec.AmountSpec amountSpec) {
        return createOffer(amountSpec, PriceQuote.fromFiatPrice(50_000, "USD"));
    }

    private static MuSigOffer createOffer(bisq.offer.amount.spec.AmountSpec amountSpec, PriceQuote price) {
        Market market = new Market("BTC", "USD", "Bitcoin", "US Dollar");
        PaymentMethodSpec<?> nonBtcPaymentMethodSpec = PaymentMethodSpecUtil.createPaymentMethodSpec(
                FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ACH_TRANSFER),
                "USD");
        return new MuSigOffer("test-id",
                OFFER_MAKER_NETWORK_ID,
                Direction.BUY,
                market,
                amountSpec,
                new FixPriceSpec(price),
                List.of(nonBtcPaymentMethodSpec.getPaymentMethod()),
                List.of(),
                "1.0.0");
    }

    private static NetworkId createNetworkId(String keyIdSuffix, int port, KeyPair keyPair) {
        AddressByTransportTypeMap addresses = new AddressByTransportTypeMap(Map.of(
                TransportType.CLEAR, new ClearnetAddress("127.0.0.1", port)));
        KeyPair pair = keyPair != null ? keyPair : KeyGeneration.generateDefaultEcKeyPair();
        PubKey pubKey = new PubKey(pair.getPublic(), "test-key-" + keyIdSuffix);
        return new NetworkId(addresses, pubKey);
    }
}
