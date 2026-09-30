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

package bisq.trade.bisq_easy;

import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.account.payment_method.BitcoinPaymentMethodSpec;
import bisq.account.payment_method.BitcoinPaymentRail;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentMethodSpec;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.account.protocol_type.TradeProtocolType;
import bisq.bonded_roles.release.AppType;
import bisq.common.market.MarketRepository;
import bisq.common.network.Address;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.contract.ContractSignatureData;
import bisq.network.identity.NetworkId;
import bisq.offer.Direction;
import bisq.offer.amount.spec.QuoteSideFixedAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.trade.ServiceProvider;
import bisq.user.profile.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A take offer request for an offer no longer in the offerbook is matched against trades. A trade is
 * created from the taker's contract before verification, so only a trade with our contract signature
 * proves that we made the offer.
 */
class BisqEasyTradeServiceSignedTradeWithOfferTest {
    private static final long OFFER_DATE = System.currentTimeMillis();

    private BisqEasyTradeService service;
    private NetworkId makerNetworkId;
    private BisqEasyOffer takersOffer;

    @BeforeEach
    void setUp() {
        service = new BisqEasyTradeService(mock(ServiceProvider.class, RETURNS_DEEP_STUBS), AppType.DESKTOP);
        makerNetworkId = networkId("maker");
        takersOffer = offer("offer-id", makerNetworkId, 100_0000, "2.1.13");
    }

    @Test
    void tradeUnderVerificationHoldsTheTakersOfferWithoutSignature() {
        addTrade(takersOffer, false);

        assertFalse(service.hasSignedTradeWithOffer(takersOffer));
    }

    @Test
    void offerSharingOnlyTheIdWithASignedTradesOfferDoesNotMatch() {
        addTrade(offer("offer-id", makerNetworkId, 200_0000, "2.1.13"), true);

        assertFalse(service.hasSignedTradeWithOffer(takersOffer));
    }

    @Test
    void offerOfAnotherMakerDoesNotMatch() {
        addTrade(offer("offer-id", networkId("other"), 100_0000, "2.1.13"), true);

        assertFalse(service.hasSignedTradeWithOffer(takersOffer));
    }

    @Test
    void equalOfferOfASignedTradeMatches() {
        // A second taker sends its own copy of the offer, possibly serialized by another app version.
        addTrade(offer("offer-id", makerNetworkId, 100_0000, "2.1.12"), true);

        assertTrue(service.hasSignedTradeWithOffer(takersOffer));
    }

    @Test
    void equalOfferOfAClosedSignedTradeMatches() {
        BisqEasyTrade trade = addTrade(offer("offer-id", makerNetworkId, 100_0000, "2.1.13"), true);
        service.closeTrade(trade, mock(UserProfile.class), mock(UserProfile.class));

        assertTrue(service.hasSignedTradeWithOffer(takersOffer));
    }

    private BisqEasyTrade addTrade(BisqEasyOffer offer, boolean signedByMaker) {
        BisqEasyTrade trade = mock(BisqEasyTrade.class, RETURNS_DEEP_STUBS);
        when(trade.getId()).thenReturn("trade-" + System.nanoTime());
        when(trade.getOffer()).thenReturn(offer);
        when(trade.getMaker().getContractSignatureData().get())
                .thenReturn(signedByMaker ? mock(ContractSignatureData.class) : null);
        service.getPersistableStore().addTrade(trade);
        return trade;
    }

    private static NetworkId networkId(String keyId) {
        Address address = Address.from("127.0.0.1", 1000);
        return new NetworkId(new AddressByTransportTypeMap(Map.of(address.getTransportType(), address)),
                new PubKey(KeyGeneration.generateDefaultEcKeyPair().getPublic(), keyId));
    }

    private static BisqEasyOffer offer(String id, NetworkId makerNetworkId, long quoteAmount, String appVersion) {
        return new BisqEasyOffer(id,
                OFFER_DATE,
                makerNetworkId,
                Direction.SELL,
                MarketRepository.getUSDBitcoinMarket(),
                new QuoteSideFixedAmountSpec(quoteAmount),
                new MarketPriceSpec(),
                List.of(TradeProtocolType.BISQ_EASY),
                List.of(new BitcoinPaymentMethodSpec(BitcoinPaymentMethod.fromPaymentRail(BitcoinPaymentRail.MAIN_CHAIN))),
                List.of(new FiatPaymentMethodSpec(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA))),
                List.of(),
                List.of("en"),
                1,
                "1.0.0",
                appVersion);
    }
}
