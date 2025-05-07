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

package bisq.trade.protocol.handler;

import bisq.network.SendMessageResult;
import bisq.trade.ServiceProvider;
import bisq.trade.Trade;
import bisq.trade.protocol.messages.TradeMessage;
import bisq.user.banned.BannedUserService;

import java.util.concurrent.CompletableFuture;

public interface TradeMessageSender<M extends Trade<?, ?, ?>> {
    default CompletableFuture<SendMessageResult> sendMessage(TradeMessage message, ServiceProvider serviceProvider, M trade) {
        BannedUserService bannedUserService = serviceProvider.getUserService().getBannedUserService();
        // If one of the twt traders is banned we block any trade message sending
        if (bannedUserService.isUserProfileBanned(message.getSender()) ||
                bannedUserService.isUserProfileBanned(trade.getPeer().getNetworkId())) {
            return CompletableFuture.failedFuture(new RuntimeException());
        }

        return serviceProvider.getNetworkService().confidentialSend(message,
                        trade.getPeer().getNetworkId(),
                trade.getMyIdentity().getNetworkIdWithKeyPair());
    }
}
