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

import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.common.monetary.PriceQuote;
import bisq.common.observable.collection.ObservableSet;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.identity.Identity;
import bisq.network.identity.NetworkId;
import bisq.offer.amount.spec.QuoteSideFixedAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.spec.FixPriceSpec;
import bisq.trade.ServiceProvider;
import bisq.trade.exceptions.TradeProtocolException;
import bisq.trade.exceptions.TradeProtocolFailure;
import org.junit.jupiter.api.Test;

import static bisq.trade.bisq_easy.protocol.messages.BisqEasyTakeOfferRequestValidator.validate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BisqEasyTakeOfferRequestValidatorTest {
    @Test
    void verifyRejectsTakeOfferRequestFromIgnoredTaker() {
        String takersProfileId = "takersProfileId";
        ServiceProvider serviceProvider = mock(ServiceProvider.class, RETURNS_DEEP_STUBS);
        when(serviceProvider.getUserService().getUserProfileService().isChatUserIgnored(takersProfileId))
                .thenReturn(true);
        when(serviceProvider.getChatService().getBisqEasyOfferbookChannelService().getChannels())
                .thenReturn(new ObservableSet<>());

        BisqEasyContract takersContract = mock(BisqEasyContract.class);
        when(takersContract.getOffer()).thenReturn(mock(BisqEasyOffer.class));
        BisqEasyTakeOfferRequest message = mock(BisqEasyTakeOfferRequest.class, RETURNS_DEEP_STUBS);
        when(message.getBisqEasyContract()).thenReturn(takersContract);
        when(message.getSender().getId()).thenReturn(takersProfileId);

        Identity myIdentity = mock(Identity.class);

        TradeProtocolException exception =
                assertThrows(TradeProtocolException.class, () -> validate(serviceProvider, myIdentity, message));
        // The rejection sent to the taker must not disclose that they have been ignored: it is the rejection for an offer
        // which the maker does not have, thrown from the same line
        assertEquals(TradeProtocolFailure.NO_MATCHING_OFFER_FOUND, exception.getTradeProtocolFailure());
        assertFalse(exception.getMessage().toLowerCase().contains("ignor"));

        when(serviceProvider.getUserService().getUserProfileService().isChatUserIgnored(takersProfileId))
                .thenReturn(false);
        TradeProtocolException notFound =
                assertThrows(TradeProtocolException.class, () -> validate(serviceProvider, myIdentity, message));
        assertEquals(notFound.getTradeProtocolFailure(), exception.getTradeProtocolFailure());
        assertEquals(notFound.getMessage(), exception.getMessage());
        assertEquals(notFound.getStackTrace()[0], exception.getStackTrace()[0]);
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
        // to the base side multiplies by 10^8 and overflows a long. The contract keeps the offer's price spec.
        FixPriceSpec priceSpec = new FixPriceSpec(PriceQuote.fromFiatPrice(0.0001, "USD"));
        when(takersOffer.getPriceSpec()).thenReturn(priceSpec);
        when(takersContract.getPriceSpec()).thenReturn(priceSpec);

        NetworkId takerNetworkId = mock(NetworkId.class, RETURNS_DEEP_STUBS);
        when(takersContract.getTaker().getNetworkId()).thenReturn(takerNetworkId);

        BisqEasyTakeOfferRequest message = mock(BisqEasyTakeOfferRequest.class, RETURNS_DEEP_STUBS);
        when(message.getBisqEasyContract()).thenReturn(takersContract);
        when(message.getSender()).thenReturn(takerNetworkId);

        // The offer is matched through a trade whose contract we have signed.
        when(serviceProvider.getChatService().getBisqEasyOfferbookChannelService().getChannels())
                .thenReturn(new ObservableSet<>());
        when(serviceProvider.getBisqEasyTradeService().hasSignedTradeWithOffer(takersOffer)).thenReturn(true);
        when(serviceProvider.getContractService().arePublicKeysMatching(any(java.security.PublicKey.class), any(java.security.PublicKey.class))).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> validate(serviceProvider, mock(Identity.class), message));
        assertTrue(exception.getMessage().contains("take offer request"));
    }
}
