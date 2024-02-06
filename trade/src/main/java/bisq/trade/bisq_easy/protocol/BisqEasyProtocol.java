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

package bisq.trade.bisq_easy.protocol;

import bisq.common.fsm.Event;
import bisq.common.fsm.EventHandler;
import bisq.common.fsm.FsmException;
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.protocol.TradeProtocol;

import java.lang.reflect.InvocationTargetException;

public abstract class BisqEasyProtocol extends TradeProtocol<BisqEasyTrade> {
    private static final String version = "1.0.0";

    public BisqEasyProtocol(ServiceProvider serviceProvider, BisqEasyTrade model) {
        super(version, serviceProvider, model);
    }

    @Override
    protected EventHandler newEventHandlerFromClass(Class<? extends EventHandler> handlerClass) {
        try {
            return handlerClass.getDeclaredConstructor(ServiceProvider.class, BisqEasyTrade.class).newInstance(serviceProvider, model);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void handle(Event event) {
        try {
            super.handle(event);
        } catch (FsmException fsmException) {
            // We swallow the exception as we handle exceptions as an error state.
            // The client need to listen for that state for error handling.
            // In case we get the error from the network message throwing the exception to the caller
            // (network layer) would not make sense. As we do not want to distinguish at the client if the error came
            // from a message or from a user event we prefer to use the same mechanism for both sources and
            // therefor do not throw an exception either.
        }
    }
}
