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

import bisq.network.SendMessageResult;
import bisq.trade.ServiceProvider;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.protocol.handler.SendTradeMessageHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
public abstract class MuSigSendTradeMessageHandler<M extends MuSigTrade> extends SendTradeMessageHandler<M> {

    protected MuSigSendTradeMessageHandler(ServiceProvider serviceProvider, M model) {
        super(serviceProvider, model);
    }

    protected Optional<CompletableFuture<SendMessageResult>> sendTradeLogMessage(String encoded) {
        return serviceProvider.getChatService().getMuSigOpenTradeChannelService().findChannelByTradeId(trade.getId())
                .map(channel ->
                        serviceProvider.getChatService().getMuSigOpenTradeChannelService().sendTradeLogMessage(encoded, channel));
    }
}