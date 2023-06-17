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

package bisq.trade_protocol.tasks;

import bisq.trade_protocol.TradeModel;
import bisq.trade_protocol.bisq_easy.ServiceProvider;
import bisq.trade_protocol.fsm.EventHandler;

public abstract class TradeProtocolEventHandler<M extends TradeModel<?, ?>> implements EventHandler {
    protected final ServiceProvider serviceProvider;
    protected final M model;

    protected TradeProtocolEventHandler(ServiceProvider serviceProvider, M model) {
        this.serviceProvider = serviceProvider;
        this.model = model;
    }
}