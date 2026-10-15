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
import bisq.offer.bisq_easy.BisqEasyOffer;

import java.util.Optional;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Offerbook channels for the tests of the offer check of a take offer request.
 */
final class OfferbookTestData {
    private OfferbookTestData() {
    }

    // A channel with one offerbook message of the author, which carries the offer
    static BisqEasyOfferbookChannel offerbookChannel(String authorUserProfileId, BisqEasyOffer offer) {
        BisqEasyOfferbookMessage offerbookMessage = mock(BisqEasyOfferbookMessage.class);
        when(offerbookMessage.getBisqEasyOffer()).thenReturn(Optional.of(offer));
        when(offerbookMessage.getAuthorUserProfileId()).thenReturn(authorUserProfileId);
        BisqEasyOfferbookChannel channel = mock(BisqEasyOfferbookChannel.class);
        when(channel.getChatMessages()).thenReturn(new ObservableSet<>(Set.of(offerbookMessage)));
        return channel;
    }
}
