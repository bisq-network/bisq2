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
import bisq.common.observable.collection.ObservableSet;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.identity.Identity;
import bisq.network.identity.NetworkId;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.trade.ServiceProvider;
import bisq.trade.exceptions.TradeProtocolException;
import bisq.trade.exceptions.TradeProtocolFailure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static bisq.trade.bisq_easy.protocol.messages.BisqEasyTakeOfferRequestValidator.validate;
import static bisq.trade.bisq_easy.protocol.messages.OfferbookTestData.offerbookChannel;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The taker's offer must be found in an offerbook message we authored.
 */
class BisqEasyTakeOfferRequestOfferMatchingTest {
    private static final String MY_PROFILE_ID = "my-profile-id";

    private ServiceProvider serviceProvider;
    private BisqEasyContract takersContract;
    private BisqEasyTakeOfferRequest message;
    private BisqEasyOffer takersOffer;
    private ObservableSet<BisqEasyOfferbookChannel> offerbookChannels;
    private Identity myIdentity;

    @BeforeEach
    void setUp() {
        serviceProvider = mock(ServiceProvider.class, RETURNS_DEEP_STUBS);
        offerbookChannels = new ObservableSet<>();
        when(serviceProvider.getChatService().getBisqEasyOfferbookChannelService().getChannels())
                .thenReturn(offerbookChannels);

        NetworkId myNetworkId = mock(NetworkId.class);
        when(myNetworkId.getId()).thenReturn(MY_PROFILE_ID);
        takersOffer = mock(BisqEasyOffer.class);
        when(takersOffer.getMakerNetworkId()).thenReturn(myNetworkId);
        when(takersOffer.getMakersUserProfileId()).thenReturn(MY_PROFILE_ID);
        takersContract = mock(BisqEasyContract.class, RETURNS_DEEP_STUBS);
        when(takersContract.getOffer()).thenReturn(takersOffer);
        message = mock(BisqEasyTakeOfferRequest.class, RETURNS_DEEP_STUBS);
        when(message.getBisqEasyContract()).thenReturn(takersContract);

        myIdentity = mock(Identity.class);
        when(myIdentity.getNetworkId()).thenReturn(myNetworkId);
    }

    @Test
    void verifyRejectsOfferNotInOfferbook() {
        TradeProtocolException exception =
                assertThrows(TradeProtocolException.class, () -> validate(serviceProvider, myIdentity, message));
        assertEquals(TradeProtocolFailure.NO_MATCHING_OFFER_FOUND, exception.getTradeProtocolFailure());
    }

    @Test
    void verifyMatchesOfferbookMessageWeAuthored() {
        offerbookChannels.add(offerbookChannel(MY_PROFILE_ID, takersOffer));

        // The remaining checks run against deep stubs and fail; the offer match itself has passed.
        assertThrows(RuntimeException.class, () -> validate(serviceProvider, myIdentity, message));
        verify(takersContract).getPriceSpec();
    }

    // The address map of our NetworkId can change after we published the offer, for example when we add a transport.
    // The offer is still ours: we compare the key.
    @Test
    void verifyMatchesOurOfferAfterOurAddressesChanged() {
        NetworkId makerNetworkIdAtPublishing = mock(NetworkId.class);
        when(makerNetworkIdAtPublishing.getId()).thenReturn(MY_PROFILE_ID);
        when(takersOffer.getMakerNetworkId()).thenReturn(makerNetworkIdAtPublishing);
        offerbookChannels.add(offerbookChannel(MY_PROFILE_ID, takersOffer));

        assertThrows(RuntimeException.class, () -> validate(serviceProvider, myIdentity, message));
        verify(takersContract).getPriceSpec();
    }
}
