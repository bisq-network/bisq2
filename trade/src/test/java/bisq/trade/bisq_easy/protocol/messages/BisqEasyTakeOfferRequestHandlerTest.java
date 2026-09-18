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

package bisq.trade.bisq_easy.protocol.messages;

import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.account.payment_method.BitcoinPaymentMethodSpec;
import bisq.account.payment_method.BitcoinPaymentRail;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentMethodSpec;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.account.protocol_type.TradeProtocolType;
import bisq.bonded_roles.BondedRolesService;
import bisq.bonded_roles.market_price.MarketPrice;
import bisq.bonded_roles.market_price.MarketPriceProvider;
import bisq.bonded_roles.market_price.MarketPriceProviderInfo;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.ChatService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannelService;
import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.common.monetary.PriceQuote;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.TransportType;
import bisq.common.network.clear_net_address_types.LocalHostAddressTypeFacade;
import bisq.common.observable.ReadOnlyObservable;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.observable.map.ObservableHashMap;
import bisq.contract.ContractService;
import bisq.contract.ContractSignatureData;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.i18n.Res;
import bisq.identity.Identity;
import bisq.network.identity.NetworkId;
import bisq.offer.Direction;
import bisq.offer.amount.spec.BaseSideFixedAmountSpec;
import bisq.offer.amount.spec.BaseSideRangeAmountSpec;
import bisq.offer.amount.spec.QuoteSideFixedAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.spec.FixPriceSpec;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.security.SignatureUtil;
import bisq.security.keys.I2PKeyGeneration;
import bisq.security.keys.KeyBundle;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.security.keys.TorKeyGeneration;
import bisq.security.keys.TorKeyPair;
import bisq.security.pow.ProofOfWork;
import bisq.settings.SettingsService;
import bisq.support.SupportService;
import bisq.support.mediation.bisq_easy.BisqEasyMediationRequestService;
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.BisqEasyTradeService;
import bisq.trade.exceptions.TradeProtocolException;
import bisq.trade.exceptions.TradeProtocolFailure;
import bisq.user.UserService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BisqEasyTakeOfferRequestHandlerTest {

    private static final Market MARKET = new Market("BTC", "USD", "Bitcoin", "US Dollar");
    private static final long TAKE_OFFER_DATE = 1_700_000_000_000L;
    private static Object previousUserProfileServiceInstance;
    private static Object previousResLocale;
    private static Object previousResBundles;
    private static final PriceQuote PRICE_60K = PriceQuote.fromFiatPrice(60_000, "USD");
    // At $60,000/BTC: 0.001 BTC (100_000 sat) costs $60 (600_000 in 4-decimal fiat units)
    private static final long CONSISTENT_BASE = 100_000L;
    private static final long CONSISTENT_QUOTE = 600_000L;

    private ServiceProvider serviceProvider;
    private BisqEasyTradeService tradeService;
    private BisqEasyMediationRequestService mediationRequestService;
    private MarketPriceService marketPriceService;

    private NetworkId makerNetworkId;
    private NetworkId takerNetworkId;
    private Identity makerIdentity;
    private BisqEasyOffer offer;
    private UserProfile mediator;

    @BeforeAll
    static void initGlobals() throws Exception {
        previousResLocale = staticField(Res.class, "locale").get(null);
        previousResBundles = staticField(Res.class, "bundles").get(null);
        Res.setAndApplyLanguageTag("en");

        UserProfileService ups = mock(UserProfileService.class);
        when(ups.evaluateUserName(any(), any())).thenAnswer(inv -> inv.getArgument(0));
        Field field = staticField(UserProfileService.class, "instance");
        previousUserProfileServiceInstance = field.get(null);
        field.set(null, ups);
    }

    @AfterAll
    static void restoreGlobals() throws Exception {
        staticField(UserProfileService.class, "instance").set(null, previousUserProfileServiceInstance);
        staticField(Res.class, "locale").set(null, previousResLocale);
        staticField(Res.class, "bundles").set(null, previousResBundles);
    }

    /**
     * Res and UserProfileService keep process-wide state in static fields without accessors, so
     * what this class overwrites is captured and put back for the test classes running after it.
     */
    private static Field staticField(Class<?> type, String name) throws ReflectiveOperationException {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    @BeforeEach
    void setUp() {
        makerNetworkId = createNetworkId(5001);
        takerNetworkId = createNetworkId(5002);
        makerIdentity = createIdentity("maker", makerNetworkId);
        mediator = createUserProfile(5003);

        offer = createOffer(makerNetworkId);

        serviceProvider = mock(ServiceProvider.class);

        ChatService chatService = mock(ChatService.class);
        BisqEasyOfferbookChannelService offerbookService = mock(BisqEasyOfferbookChannelService.class);
        when(serviceProvider.getChatService()).thenReturn(chatService);
        when(chatService.getBisqEasyOfferbookChannelService()).thenReturn(offerbookService);
        when(offerbookService.getChannels()).thenReturn(new ObservableSet<>());

        tradeService = mock(BisqEasyTradeService.class);
        when(serviceProvider.getBisqEasyTradeService()).thenReturn(tradeService);

        UserService userService = mock(UserService.class);
        UserProfileService userProfileService = mock(UserProfileService.class);
        when(serviceProvider.getUserService()).thenReturn(userService);
        when(userService.getUserProfileService()).thenReturn(userProfileService);
        when(userProfileService.isChatUserIgnored(any(String.class))).thenReturn(false);

        SettingsService settingsService = mock(SettingsService.class);
        when(serviceProvider.getSettingsService()).thenReturn(settingsService);
        @SuppressWarnings("unchecked")
        ReadOnlyObservable<Boolean> closeOfferObs = mock(ReadOnlyObservable.class);
        when(closeOfferObs.get()).thenReturn(true);
        when(settingsService.getCloseMyOfferWhenTaken()).thenReturn(closeOfferObs);
        @SuppressWarnings("unchecked")
        ReadOnlyObservable<Double> deviationObs = mock(ReadOnlyObservable.class);
        when(deviationObs.get()).thenReturn(0.1);
        when(settingsService.getMaxTradePriceDeviation()).thenReturn(deviationObs);

        SupportService supportService = mock(SupportService.class);
        mediationRequestService = mock(BisqEasyMediationRequestService.class);
        when(serviceProvider.getSupportService()).thenReturn(supportService);
        when(supportService.getBisqEasyMediationRequestService()).thenReturn(mediationRequestService);
        when(mediationRequestService.selectMediator(any(), any(), any()))
                .thenReturn(Optional.of(mediator));

        ContractService contractService = mock(ContractService.class);
        when(serviceProvider.getContractService()).thenReturn(contractService);
        when(contractService.arePublicKeysMatching(any(PublicKey.class), any(PublicKey.class))).thenReturn(true);
        when(contractService.arePublicKeysMatching(any(ContractSignatureData.class), any(PublicKey.class))).thenReturn(true);
        try {
            when(contractService.verifyContractSignature(any(BisqEasyContract.class), any(ContractSignatureData.class)))
                    .thenReturn(true);
        } catch (GeneralSecurityException ignored) {
        }

        BondedRolesService bondedRolesService = mock(BondedRolesService.class);
        marketPriceService = mock(MarketPriceService.class);
        when(serviceProvider.getBondedRolesService()).thenReturn(bondedRolesService);
        when(bondedRolesService.getMarketPriceService()).thenReturn(marketPriceService);

        MarketPrice marketPrice = new MarketPrice(PRICE_60K, System.currentTimeMillis(),
                new MarketPriceProviderInfo(MarketPriceProvider.BISQAGGREGATE));
        ObservableHashMap<Market, MarketPrice> priceMap = new ObservableHashMap<>();
        priceMap.put(MARKET, marketPrice);
        when(marketPriceService.getMarketPriceByCurrencyMap()).thenReturn(priceMap);
        when(marketPriceService.findMarketPrice(MARKET)).thenReturn(Optional.of(marketPrice));
        when(marketPriceService.findMarketPriceQuote(MARKET)).thenReturn(Optional.of(PRICE_60K));

        stubTradeServiceWithMatchingOffer();
    }

    @Test
    void noMatchingOfferThrowsNoMatchingOfferFound() {
        when(tradeService.getTrades()).thenReturn(new ObservableSet<>());

        BisqEasyContract contract = createContract(CONSISTENT_BASE, CONSISTENT_QUOTE);
        BisqEasyTrade trade = createMakerTrade(contract, false);
        BisqEasyTakeOfferRequestHandler handler = new BisqEasyTakeOfferRequestHandler(serviceProvider, trade);
        BisqEasyTakeOfferRequest message = createMessage(contract, trade);

        TradeProtocolException ex = assertThrows(TradeProtocolException.class, () -> handler.verify(message));
        assertEquals(TradeProtocolFailure.NO_MATCHING_OFFER_FOUND, ex.getTradeProtocolFailure());
    }

    @Test
    void senderIdentityMismatchThrows() {
        try {
            NetworkId wrongSender = createNetworkId(9999);
            BisqEasyContract contract = createContract(CONSISTENT_BASE, CONSISTENT_QUOTE);
            BisqEasyTrade trade = createMakerTrade(contract, false);

            KeyPair sigKp = KeyGeneration.generateDefaultEcKeyPair();
            byte[] contractHash = ContractService.getContractHash(contract);
            byte[] signature = SignatureUtil.sign(contractHash, sigKp.getPrivate());
            ContractSignatureData sigData = new ContractSignatureData(contractHash, signature, sigKp.getPublic());
            BisqEasyTakeOfferRequest message = new BisqEasyTakeOfferRequest(
                    "msg-1", trade.getId(), "1.0.0",
                    wrongSender, makerNetworkId, contract, sigData);

            BisqEasyTakeOfferRequestHandler handler = new BisqEasyTakeOfferRequestHandler(serviceProvider, trade);
            assertThrows(IllegalArgumentException.class, () -> handler.verify(message));
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void paymentMethodNotInOfferThrows() {
        BisqEasyContract contract = new BisqEasyContract(
                TAKE_OFFER_DATE, offer, takerNetworkId, CONSISTENT_BASE, CONSISTENT_QUOTE,
                new BitcoinPaymentMethodSpec(BitcoinPaymentMethod.fromPaymentRail(BitcoinPaymentRail.MAIN_CHAIN)),
                new FiatPaymentMethodSpec(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ZELLE)),
                Optional.of(mediator), new FixPriceSpec(PRICE_60K), CONSISTENT_QUOTE);

        BisqEasyTrade trade = createMakerTrade(contract, false);
        BisqEasyTakeOfferRequestHandler handler = new BisqEasyTakeOfferRequestHandler(serviceProvider, trade);
        BisqEasyTakeOfferRequest message = createMessage(contract, trade);

        assertThrows(IllegalArgumentException.class, () -> handler.verify(message));
    }

    @Test
    void mediatorMismatchThrowsMediatorsNotMatching() {
        UserProfile differentMediator = createUserProfile(7777);
        when(mediationRequestService.selectMediator(any(), any(), any()))
                .thenReturn(Optional.of(differentMediator));

        BisqEasyContract contract = createContract(CONSISTENT_BASE, CONSISTENT_QUOTE);
        BisqEasyTrade trade = createMakerTrade(contract, false);
        BisqEasyTakeOfferRequestHandler handler = new BisqEasyTakeOfferRequestHandler(serviceProvider, trade);
        BisqEasyTakeOfferRequest message = createMessage(contract, trade);

        TradeProtocolException ex = assertThrows(TradeProtocolException.class, () -> handler.verify(message));
        assertEquals(TradeProtocolFailure.MEDIATORS_NOT_MATCHING, ex.getTradeProtocolFailure());
    }

    @Test
    void buyerRejectsWhenTakersBtcAmountIsTooLow() {
        long tooLowBaseSide = (long) (CONSISTENT_BASE * 0.85);
        useRangeOffer();

        BisqEasyContract contract = createContract(tooLowBaseSide, CONSISTENT_QUOTE);
        BisqEasyTrade trade = createMakerTrade(contract, true);
        BisqEasyTakeOfferRequestHandler handler = new BisqEasyTakeOfferRequestHandler(serviceProvider, trade);
        BisqEasyTakeOfferRequest message = createMessage(contract, trade);

        TradeProtocolException ex = assertThrows(TradeProtocolException.class, () -> handler.verify(message));
        assertEquals(TradeProtocolFailure.PRICE_DEVIATION, ex.getTradeProtocolFailure());
    }

    @Test
    void sellerRejectsWhenTakersBtcAmountIsTooHigh() {
        long tooHighBaseSide = (long) (CONSISTENT_BASE * 1.15);
        useRangeOffer();

        BisqEasyContract contract = createContract(tooHighBaseSide, CONSISTENT_QUOTE);
        BisqEasyTrade trade = createMakerTrade(contract, false);
        BisqEasyTakeOfferRequestHandler handler = new BisqEasyTakeOfferRequestHandler(serviceProvider, trade);
        BisqEasyTakeOfferRequest message = createMessage(contract, trade);

        TradeProtocolException ex = assertThrows(TradeProtocolException.class, () -> handler.verify(message));
        assertEquals(TradeProtocolFailure.PRICE_DEVIATION, ex.getTradeProtocolFailure());
    }

    @Test
    void validTakeOfferRequestPassesAllVerifyChecks() {
        BisqEasyContract contract = createContract(CONSISTENT_BASE, CONSISTENT_QUOTE);
        BisqEasyTrade trade = createMakerTrade(contract, false);
        BisqEasyTakeOfferRequestHandler handler = new BisqEasyTakeOfferRequestHandler(serviceProvider, trade);
        BisqEasyTakeOfferRequest message = createMessage(contract, trade);

        assertDoesNotThrow(() -> handler.verify(message));
    }

    @Test
    void verifyRejectsTakeOfferRequestFromIgnoredTaker() {
        String takersProfileId = "takersProfileId";
        ServiceProvider serviceProvider = mock(ServiceProvider.class, RETURNS_DEEP_STUBS);
        when(serviceProvider.getUserService().getUserProfileService().isChatUserIgnored(takersProfileId))
                .thenReturn(true);

        BisqEasyContract takersContract = mock(BisqEasyContract.class);
        when(takersContract.getOffer()).thenReturn(mock(BisqEasyOffer.class));
        BisqEasyTakeOfferRequest message = mock(BisqEasyTakeOfferRequest.class, RETURNS_DEEP_STUBS);
        when(message.getBisqEasyContract()).thenReturn(takersContract);
        when(message.getSender().getId()).thenReturn(takersProfileId);

        BisqEasyTakeOfferRequestHandler handler =
                new BisqEasyTakeOfferRequestHandler(serviceProvider, mock(BisqEasyTrade.class));

        TradeProtocolException exception =
                assertThrows(TradeProtocolException.class, () -> handler.verify(message));
        // The rejection reason sent to the taker must not disclose that they have been ignored
        assertEquals(TradeProtocolFailure.OFFER_NOT_AVAILABLE, exception.getTradeProtocolFailure());
        assertFalse(exception.getMessage().toLowerCase().contains("ignor"));
    }

    @Test
    void verifyRejectsTakeOfferRequestWithOverflowingAmountConversion() {
        ServiceProvider serviceProvider = mock(ServiceProvider.class, RETURNS_DEEP_STUBS);
        Market market = MarketRepository.getUSDBitcoinMarket();

        BisqEasyOffer takersOffer = mock(BisqEasyOffer.class);
        when(takersOffer.getId()).thenReturn("offer-id");
        when(takersOffer.getMarket()).thenReturn(market);
        when(takersOffer.getAmountSpec()).thenReturn(new QuoteSideFixedAmountSpec(Long.MAX_VALUE));

        BisqEasyContract takersContract = mock(BisqEasyContract.class, RETURNS_DEEP_STUBS);
        when(takersContract.getOffer()).thenReturn(takersOffer);
        when(takersContract.getBaseSideAmount()).thenReturn(1L);
        when(takersContract.getQuoteSideAmount()).thenReturn(Long.MAX_VALUE);
        // A fixed price of one quote unit per BTC: converting the Long.MAX_VALUE quote amount
        // to the base side multiplies by 10^8 and overflows a long.
        when(takersContract.getPriceSpec()).thenReturn(new FixPriceSpec(PriceQuote.fromFiatPrice(0.0001, "USD")));

        NetworkId takerNetworkId = mock(NetworkId.class, RETURNS_DEEP_STUBS);
        when(takersContract.getTaker().getNetworkId()).thenReturn(takerNetworkId);

        BisqEasyTakeOfferRequest message = mock(BisqEasyTakeOfferRequest.class, RETURNS_DEEP_STUBS);
        when(message.getBisqEasyContract()).thenReturn(takersContract);
        when(message.getSender()).thenReturn(takerNetworkId);

        // The offer is matched through an existing trade with the same offer id.
        when(serviceProvider.getChatService().getBisqEasyOfferbookChannelService().getChannels())
                .thenReturn(new ObservableSet<>());
        BisqEasyTrade existingTrade = mock(BisqEasyTrade.class, RETURNS_DEEP_STUBS);
        when(existingTrade.getOffer().getId()).thenReturn("offer-id");
        ObservableSet<BisqEasyTrade> trades = new ObservableSet<>();
        trades.add(existingTrade);
        when(serviceProvider.getBisqEasyTradeService().getTrades()).thenReturn(trades);
        when(serviceProvider.getContractService().arePublicKeysMatching(any(java.security.PublicKey.class), any(java.security.PublicKey.class))).thenReturn(true);

        BisqEasyTakeOfferRequestHandler handler =
                new BisqEasyTakeOfferRequestHandler(serviceProvider, mock(BisqEasyTrade.class, RETURNS_DEEP_STUBS));

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> handler.verify(message));
        assertTrue(exception.getMessage().contains("take offer request"));
    }

    private BisqEasyContract createContract(long baseSideAmount, long quoteSideAmount) {
        return new BisqEasyContract(
                TAKE_OFFER_DATE, offer, takerNetworkId,
                baseSideAmount, quoteSideAmount,
                new BitcoinPaymentMethodSpec(BitcoinPaymentMethod.fromPaymentRail(BitcoinPaymentRail.MAIN_CHAIN)),
                new FiatPaymentMethodSpec(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA)),
                Optional.of(mediator), new FixPriceSpec(PRICE_60K), CONSISTENT_QUOTE);
    }

    private BisqEasyTrade createMakerTrade(BisqEasyContract contract, boolean isBuyer) {
        return new BisqEasyTrade(contract, isBuyer, false, makerIdentity, offer, takerNetworkId, makerNetworkId);
    }

    private BisqEasyTakeOfferRequest createMessage(BisqEasyContract contract, BisqEasyTrade trade) {
        try {
            KeyPair sigKp = KeyGeneration.generateDefaultEcKeyPair();
            byte[] contractHash = ContractService.getContractHash(contract);
            byte[] signature = SignatureUtil.sign(contractHash, sigKp.getPrivate());
            ContractSignatureData sigData = new ContractSignatureData(contractHash, signature, sigKp.getPublic());
            return new BisqEasyTakeOfferRequest(
                    "msg-" + System.nanoTime(), trade.getId(), "1.0.0",
                    takerNetworkId, makerNetworkId, contract, sigData);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    private void stubTradeServiceWithMatchingOffer() {
        BisqEasyContract stubContract = createContract(CONSISTENT_BASE, CONSISTENT_QUOTE);
        BisqEasyTrade existingTrade = new BisqEasyTrade(
                stubContract, false, false, makerIdentity, offer, takerNetworkId, makerNetworkId);
        ObservableSet<BisqEasyTrade> trades = new ObservableSet<>();
        trades.add(existingTrade);
        when(tradeService.getTrades()).thenReturn(trades);
    }

    /**
     * Swaps the fixture offer for one with a base side range, so amounts that deviate from the
     * contract price reach the price deviation check instead of being rejected by the offer
     * amount validation.
     */
    private void useRangeOffer() {
        offer = new BisqEasyOffer(
                "offer-handler-range-test",
                System.currentTimeMillis(),
                makerNetworkId,
                Direction.SELL,
                MARKET,
                new BaseSideRangeAmountSpec(CONSISTENT_BASE / 2, CONSISTENT_BASE * 2),
                new FixPriceSpec(PRICE_60K),
                List.of(TradeProtocolType.BISQ_EASY),
                List.of(new BitcoinPaymentMethodSpec(BitcoinPaymentMethod.fromPaymentRail(BitcoinPaymentRail.MAIN_CHAIN))),
                List.of(new FiatPaymentMethodSpec(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA))),
                List.of(),
                List.of("en"),
                0,
                "1.0.0",
                "1.0.0"
        );
        stubTradeServiceWithMatchingOffer();
    }

    private BisqEasyOffer createOffer(NetworkId makerNid) {
        return new BisqEasyOffer(
                "offer-handler-test",
                System.currentTimeMillis(),
                makerNid,
                Direction.SELL,
                MARKET,
                new BaseSideFixedAmountSpec(CONSISTENT_BASE),
                new FixPriceSpec(PRICE_60K),
                List.of(TradeProtocolType.BISQ_EASY),
                List.of(new BitcoinPaymentMethodSpec(BitcoinPaymentMethod.fromPaymentRail(BitcoinPaymentRail.MAIN_CHAIN))),
                List.of(new FiatPaymentMethodSpec(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA))),
                List.of(),
                List.of("en"),
                0,
                "1.0.0",
                "1.0.0"
        );
    }

    private NetworkId createNetworkId(int port) {
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        PubKey pubKey = new PubKey(keyPair.getPublic(), "key-" + port);
        AddressByTransportTypeMap addresses = new AddressByTransportTypeMap(
                Map.of(TransportType.CLEAR, LocalHostAddressTypeFacade.toLocalHostAddress(port))
        );
        return new NetworkId(addresses, pubKey);
    }

    private Identity createIdentity(String tag, NetworkId networkId) {
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        TorKeyPair torKeyPair = TorKeyGeneration.generateKeyPair();
        var i2pKeyPair = I2PKeyGeneration.generateKeyPair();
        KeyBundle keyBundle = new KeyBundle(tag, keyPair, torKeyPair, i2pKeyPair);
        return new Identity(tag, networkId, keyBundle);
    }

    private UserProfile createUserProfile(int port) {
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        PubKey pubKey = new PubKey(keyPair.getPublic(), "key-" + port);
        AddressByTransportTypeMap addresses = new AddressByTransportTypeMap(
                Map.of(TransportType.CLEAR, LocalHostAddressTypeFacade.toLocalHostAddress(port))
        );
        NetworkId networkId = new NetworkId(addresses, pubKey);
        ProofOfWork proofOfWork = new ProofOfWork(pubKey.getHash(), 0, null, 1.0, new byte[72], 0);
        return new UserProfile(1, "nick-" + port, proofOfWork, 0, networkId, "", "", "1.0.0");
    }
}
