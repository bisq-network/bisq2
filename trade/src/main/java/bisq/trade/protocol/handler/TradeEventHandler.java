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

import bisq.common.fsm.Event;
import bisq.common.fsm.EventHandler;
import bisq.trade.ServiceProvider;
import bisq.trade.Trade;

public abstract class TradeEventHandler<T extends Trade<?, ?, ?>, E extends Event> implements EventHandler {
    protected final ServiceProvider serviceProvider;
    protected final T trade;

    protected TradeEventHandler(ServiceProvider serviceProvider, T trade) {
        this.serviceProvider = serviceProvider;
        this.trade = trade;
    }

    public void handle(Event event) {
        process(event);
        commit();
    }

    protected abstract void process(Event event);

    protected abstract void commit();

    @SuppressWarnings("unchecked")
    private E unsafeCast(Event event) {
        try {
            return (E) event;
        } catch (Exception e) {
            throw new ClassCastException("Could not cast event to generic Event type in " + getClass().getSimpleName() + ". " + e.getMessage());
        }
    }
}