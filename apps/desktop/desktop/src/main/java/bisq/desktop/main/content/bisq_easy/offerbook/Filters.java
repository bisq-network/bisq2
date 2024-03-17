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

package bisq.desktop.main.content.bisq_easy.offerbook;

import bisq.chat.ChatChannel;
import bisq.chat.ChatMessage;
import bisq.desktop.main.content.components.chatMessages.ChatMessageListItem;
import lombok.Getter;

import java.util.function.Predicate;

class Filters {
    interface FilterPredicate<T> {
        Predicate<T> getPredicate();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // MARKETS' FILTERS
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Getter
    enum Markets implements FilterPredicate<MarketChannelItem> {
        ALL(item -> true),
        FAVOURITES(item -> item.getIsFavourite().get()),
        WITH_OFFERS(item -> item.getNumOffers().get() > 0);

        private final Predicate<MarketChannelItem> predicate;

        Markets(Predicate<MarketChannelItem> predicate) {
            this.predicate = predicate;
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // OFFERS' FILTERS
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Getter
    enum OfferDirectionOrOwner implements FilterPredicate<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> {
        ALL(item -> true),
        MINE(item -> !item.isBisqEasyPublicChatMessageWithOffer() || item.isBisqEasyPublicChatMessageWithMyOffer()),
        BUY(item -> !item.isBisqEasyPublicChatMessageWithOffer() || item.isBisqEasyPublicChatMessageWithPeerBuyOffer()),
        SELL(item -> !item.isBisqEasyPublicChatMessageWithOffer() || item.isBisqEasyPublicChatMessageWithPeerSellOffer());

        private final Predicate<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> predicate;

        OfferDirectionOrOwner(Predicate<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> predicate) {
            this.predicate = predicate;
        }
    }

    @Getter
    enum PeerReputation implements FilterPredicate<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> {
        ALL(item -> true),
        FIVE_STARS(item -> !item.isBisqEasyPublicChatMessageWithOffer()
                || (item.isPeerMessage() && item.getReputationStarCount() == 5)),
        AT_LEAST_FOUR_STARS(item -> !item.isBisqEasyPublicChatMessageWithOffer()
                || (item.isPeerMessage() && item.getReputationStarCount() >= 4)),
        AT_LEAST_THREE_STARS(item -> !item.isBisqEasyPublicChatMessageWithOffer()
                || (item.isPeerMessage() && item.getReputationStarCount() >= 3)),
        AT_LEAST_TWO_STARS(item -> !item.isBisqEasyPublicChatMessageWithOffer()
                || (item.isPeerMessage() && item.getReputationStarCount() >= 2)),
        AT_LEAST_ONE_STAR(item -> !item.isBisqEasyPublicChatMessageWithOffer()
                || (item.isPeerMessage() && item.getReputationStarCount() >= 1));

        private final Predicate<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> predicate;

        PeerReputation(Predicate<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> predicate) {
            this.predicate = predicate;
        }
    }
}
