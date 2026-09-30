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

import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
import bisq.common.observable.collection.ObservableSet;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.network.identity.NetworkId;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.BisqEasyTradeService;
import bisq.trade.exceptions.TradeProtocolException;
import bisq.trade.exceptions.TradeProtocolFailure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The taker's offer must be found in an offerbook message we authored, or in a trade we have signed.
 */
class BisqEasyTakeOfferRequestOfferMatchingTest {
    private static final String MY_PROFILE_ID = "my-profile-id";

    private ServiceProvider serviceProvider;
    private BisqEasyTradeService tradeService;
    private BisqEasyTakeOfferRequest message;
    private BisqEasyOffer takersOffer;
    private ObservableSet<BisqEasyOfferbookChannel> offerbookChannels;
    private BisqEasyTakeOfferRequestHandler handler;

    @BeforeEach
    void setUp() {
        serviceProvider = mock(ServiceProvider.class, RETURNS_DEEP_STUBS);
        tradeService = mock(BisqEasyTradeService.class);
        when(serviceProvider.getBisqEasyTradeService()).thenReturn(tradeService);
        offerbookChannels = new ObservableSet<>();
        when(serviceProvider.getChatService().getBisqEasyOfferbookChannelService().getChannels())
                .thenReturn(offerbookChannels);

        NetworkId myNetworkId = mock(NetworkId.class);
        takersOffer = mock(BisqEasyOffer.class);
        when(takersOffer.getMakerNetworkId()).thenReturn(myNetworkId);
        when(takersOffer.getMakersUserProfileId()).thenReturn(MY_PROFILE_ID);
        BisqEasyContract takersContract = mock(BisqEasyContract.class, RETURNS_DEEP_STUBS);
        when(takersContract.getOffer()).thenReturn(takersOffer);
        message = mock(BisqEasyTakeOfferRequest.class, RETURNS_DEEP_STUBS);
        when(message.getBisqEasyContract()).thenReturn(takersContract);

        BisqEasyTrade trade = mock(BisqEasyTrade.class, RETURNS_DEEP_STUBS);
        when(trade.getMyIdentity().getNetworkId()).thenReturn(myNetworkId);
        handler = new BisqEasyTakeOfferRequestHandler(serviceProvider, trade);
    }

    @Test
    void verifyRejectsOfferNeitherInOfferbookNorInASignedTrade() {
        when(tradeService.hasSignedTradeWithOffer(takersOffer)).thenReturn(false);

        TradeProtocolException exception =
                assertThrows(TradeProtocolException.class, () -> handler.verify(message));
        assertEquals(TradeProtocolFailure.NO_MATCHING_OFFER_FOUND, exception.getTradeProtocolFailure());
    }

    @Test
    void verifyIgnoresOfferbookMessageOfAnotherAuthorCarryingOurOffer() {
        addOfferbookMessage("other-profile-id", takersOffer);
        when(tradeService.hasSignedTradeWithOffer(takersOffer)).thenReturn(false);

        TradeProtocolException exception =
                assertThrows(TradeProtocolException.class, () -> handler.verify(message));
        assertEquals(TradeProtocolFailure.NO_MATCHING_OFFER_FOUND, exception.getTradeProtocolFailure());
    }

    @Test
    void verifyMatchesOfferbookMessageWeAuthored() {
        addOfferbookMessage(MY_PROFILE_ID, takersOffer);

        // The remaining checks run against deep stubs and fail; the offer match itself has passed.
        assertThrows(RuntimeException.class, () -> handler.verify(message));
        verify(tradeService, never()).hasSignedTradeWithOffer(any());
    }

    private void addOfferbookMessage(String authorUserProfileId, BisqEasyOffer offer) {
        BisqEasyOfferbookMessage offerbookMessage = mock(BisqEasyOfferbookMessage.class);
        when(offerbookMessage.getBisqEasyOffer()).thenReturn(Optional.of(offer));
        when(offerbookMessage.getAuthorUserProfileId()).thenReturn(authorUserProfileId);
        BisqEasyOfferbookChannel channel = mock(BisqEasyOfferbookChannel.class);
        when(channel.getChatMessages()).thenReturn(new ObservableSet<>(Set.of(offerbookMessage)));
        offerbookChannels.add(channel);
    }
}
