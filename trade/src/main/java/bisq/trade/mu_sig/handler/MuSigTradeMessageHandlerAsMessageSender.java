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

package bisq.trade.mu_sig.handler;

import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannelService;
import bisq.network.SendMessageResult;
import bisq.trade.ServiceProvider;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.MuSigTradeService;
import bisq.trade.protobuf.MusigGrpc;
import bisq.trade.protocol.handler.TradeMessageHandlerAsMessageSender;
import bisq.trade.protocol.messages.TradeMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
public abstract class MuSigTradeMessageHandlerAsMessageSender<T extends MuSigTrade, M extends TradeMessage> extends TradeMessageHandlerAsMessageSender<T, M> {
    protected final MuSigTradeService tradeService;
    protected final MusigGrpc.MusigBlockingStub blockingStub;

    protected MuSigTradeMessageHandlerAsMessageSender(ServiceProvider serviceProvider, T trade) {
        super(serviceProvider, trade);

        tradeService = serviceProvider.getMuSigTradeService();
        blockingStub = tradeService.getMusigBlockingStub();
    }

    public final void handle(M message) {
        super.handle(message);

        sendLogMessage();
    }

    protected abstract void sendLogMessage();

    protected Optional<CompletableFuture<SendMessageResult>> sendLogMessage(String encoded) {
        MuSigOpenTradeChannelService openTradeChannelService = serviceProvider.getChatService().getMuSigOpenTradeChannelService();
        return openTradeChannelService.findChannelByTradeId(trade.getId())
                .map(channel ->
                        openTradeChannelService.sendTradeLogMessage(encoded, channel));
    }
}