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

package bisq.trade.protocol;

import bisq.common.fsm.Fsm;
import bisq.trade.ServiceProvider;
import bisq.trade.Trade;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@EqualsAndHashCode(callSuper = true)
public abstract class TradeProtocol<M extends Trade<?, ?, ?>> extends Fsm<M> {
    private final String version;
    protected final ServiceProvider serviceProvider;

    public TradeProtocol(String version, ServiceProvider serviceProvider, M model) {
        super(model);

        this.version = version;
        this.serviceProvider = serviceProvider;
    }
}
