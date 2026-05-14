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

package bisq.bisq_easy;

import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.account.payment_method.BitcoinPaymentMethodSpec;
import bisq.account.payment_method.BitcoinPaymentRail;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentMethodSpec;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.account.protocol_type.TradeProtocolType;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.PriceQuote;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.TransportType;
import bisq.common.network.clear_net_address_types.LocalHostAddressTypeFacade;
import bisq.network.identity.NetworkId;
import bisq.offer.Direction;
import bisq.offer.amount.spec.QuoteSideFixedAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.security.pow.ProofOfWork;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BisqEasySellersReputationBasedTradeAmountServiceTest {

    private static final Market USD_BTC_MARKET = MarketRepository.getUSDBitcoinMarket();
    private static final PriceQuote BTC_USD_PRICE = PriceQuote.fromFiatPrice(60_000, "USD");

    private NetworkId makerNetworkId;
    private String makerProfileId;
    private UserProfileService userProfileService;
    private ReputationService reputationService;
    private MarketPriceService marketPriceService;
    private BisqEasySellersReputationBasedTradeAmountService service;

    @BeforeEach
    void set_up() {
        makerNetworkId = createNetworkId(7001);
        makerProfileId = makerNetworkId.getPubKey().getId();
        userProfileService = mock(UserProfileService.class);
        reputationService = mock(ReputationService.class);
        marketPriceService = mock(MarketPriceService.class);
        service = new BisqEasySellersReputationBasedTradeAmountService(
                userProfileService, reputationService, marketPriceService);
    }

    @Test
    @DisplayName("Buy offer always returns true regardless of reputation")
    void buy_offer_always_returns_true_regardless_of_reputation() {
        BisqEasyOffer offer = createRealOffer(Direction.BUY, 100);

        assertTrue(service.hasSellerSufficientReputation(offer));
        verifyNoInteractions(userProfileService, reputationService);
    }

    @Test
    @DisplayName("Sell offer with sufficient reputation returns true")
    void sell_offer_with_sufficient_reputation_returns_true() {
        BisqEasyOffer offer = createSellOfferWithUsdAmount(100);
        stubMarketPrice();
        stubSellerReputation(25_000);

        assertTrue(service.hasSellerSufficientReputation(offer));
    }

    @Test
    @DisplayName("Sell offer with insufficient reputation returns false")
    void sell_offer_with_insufficient_reputation_returns_false() {
        BisqEasyOffer offer = createSellOfferWithUsdAmount(100);
        stubMarketPrice();
        stubSellerReputation(5_000);

        assertFalse(service.hasSellerSufficientReputation(offer));
    }

    @Test
    @DisplayName("Cached insufficient result skips reputation lookup on second call")
    void cached_insufficient_result_skips_reputation_lookup_on_second_call() {
        BisqEasyOffer offer = createSellOfferWithUsdAmount(100);
        stubMarketPrice();
        stubSellerReputation(0);

        assertFalse(service.hasSellerSufficientReputation(offer));
        verify(userProfileService, times(1)).findUserProfile(makerProfileId);

        assertFalse(service.hasSellerSufficientReputation(offer));
        verifyNoMoreInteractions(userProfileService);
    }

    @Test
    @DisplayName("Sell offer returns true when market price is unavailable")
    void sell_offer_returns_true_when_market_price_is_unavailable() {
        BisqEasyOffer offer = createSellOfferWithUsdAmount(100);
        when(marketPriceService.findMarketPriceQuote(any(Market.class))).thenReturn(Optional.empty());

        assertTrue(service.hasSellerSufficientReputation(offer));
    }

    private BisqEasyOffer createRealOffer(Direction direction, long usdFaceValue) {
        Fiat fiat = Fiat.fromFaceValue(usdFaceValue, "USD");
        return new BisqEasyOffer(
                "offer-rep-test-" + System.nanoTime(),
                System.currentTimeMillis(),
                makerNetworkId,
                direction,
                USD_BTC_MARKET,
                new QuoteSideFixedAmountSpec(fiat.getValue()),
                new MarketPriceSpec(),
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

    private BisqEasyOffer createSellOfferWithUsdAmount(long usdFaceValue) {
        return createRealOffer(Direction.SELL, usdFaceValue);
    }

    private void stubMarketPrice() {
        when(marketPriceService.findMarketPriceQuote(USD_BTC_MARKET)).thenReturn(Optional.of(BTC_USD_PRICE));
    }

    private void stubSellerReputation(long totalScore) {
        UserProfile profile = createUserProfile(8001);
        when(userProfileService.findUserProfile(makerProfileId)).thenReturn(Optional.of(profile));
        when(reputationService.getReputationScore(profile))
                .thenReturn(new ReputationScore(totalScore, 0, Integer.MAX_VALUE));
    }

    private NetworkId createNetworkId(int port) {
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        PubKey pubKey = new PubKey(keyPair.getPublic(), "key-" + port);
        AddressByTransportTypeMap addresses = new AddressByTransportTypeMap(
                Map.of(TransportType.CLEAR, LocalHostAddressTypeFacade.toLocalHostAddress(port))
        );
        return new NetworkId(addresses, pubKey);
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
