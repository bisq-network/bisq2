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

package bisq.trade.bisq_easy.validation;

import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.network.identity.NetworkId;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.exceptions.TradeProtocolException;
import bisq.trade.exceptions.TradeProtocolFailure;
import bisq.user.profile.UserProfile;

import java.util.Optional;

/**
 * The trade chat channel keeps its own copies of the offer, the mediator and the peer, and its own user identity.
 * On the maker's side all of them come from the taker's first chat message, which is unsigned. A trade may only
 * progress while its channel exists and its channel data matches the trade and its signed contract.
 * See docs/specifications/trade/chat-channel-binding.md.
 */
public final class BisqEasyTradeChatChannelValidator {
    private BisqEasyTradeChatChannelValidator() {
    }

    public static void validateChatChannel(Optional<BisqEasyOpenTradeChannel> channel, BisqEasyTrade trade) {
        if (channel.isEmpty()) {
            throw new TradeProtocolException("The trade chat channel does not exist. tradeId=" + trade.getId(),
                    TradeProtocolFailure.CHANNEL_DATA_MISMATCH);
        }
        findMismatch(channel.get(), trade).ifPresent(mismatch -> {
            throw new TradeProtocolException(mismatch, TradeProtocolFailure.CHANNEL_DATA_MISMATCH);
        });
    }

    public static Optional<String> findMismatch(BisqEasyOpenTradeChannel channel, BisqEasyTrade trade) {
        if (!channel.getBisqEasyOffer().equals(trade.getOffer())) {
            return Optional.of("The offer of the trade chat channel does not match the offer of the contract. tradeId=" + trade.getId());
        }
        // A user profile can change its terms and statement, so mediators are not compared by value. The whole network
        // ID is compared, including the addresses, because the chat sends the messages for the mediator to the addresses
        // of the channel's copy (docs/specifications/user/identity-by-key.md, rule 4).
        Optional<NetworkId> channelMediator = channel.getMediator().map(UserProfile::getNetworkId);
        Optional<NetworkId> contractMediator = trade.getContract().getMediator().map(UserProfile::getNetworkId);
        if (!channelMediator.equals(contractMediator)) {
            return Optional.of("The mediator of the trade chat channel does not match the mediator of the contract. tradeId=" + trade.getId());
        }
        // The user profile ID is the hash of the network ID's public key.
        if (!channel.getPeer().getId().equals(trade.getPeer().getNetworkId().getId())) {
            return Optional.of("The peer of the trade chat channel does not match the peer of the contract. tradeId=" + trade.getId());
        }
        // On the maker's side the channel's identity is the one the taker's first chat message is addressed to.
        // Both IDs are the hash of the own public key.
        if (!channel.getMyUserIdentity().getId().equals(trade.getMyIdentity().getId())) {
            return Optional.of("The own identity of the trade chat channel does not match the identity of the trade. tradeId=" + trade.getId());
        }
        return Optional.empty();
    }
}
