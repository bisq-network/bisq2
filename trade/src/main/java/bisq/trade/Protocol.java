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

package bisq.trade;

import bisq.common.fsm.Event;
import bisq.common.fsm.Fsm;
import bisq.common.fsm.FsmException;
import bisq.trade.bisq_easy.ServiceProvider;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@EqualsAndHashCode(callSuper = true)
public abstract class Protocol<M extends Trade<?, ?, ?>> extends Fsm {

    protected final ServiceProvider serviceProvider;
    protected final M model;

    public Protocol(ServiceProvider serviceProvider, M model) {
        super(model);
        this.serviceProvider = serviceProvider;
        this.model = model;
    }

    public void handle(Event event) throws TradeException {
        try {
            super.handle(event);
        } catch (FsmException e) {
            throw new TradeException(e);
        }
    }
}