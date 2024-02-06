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

package bisq.trade.submarine.protocol;

import bisq.common.fsm.EventHandler;
import bisq.trade.ServiceProvider;
import bisq.trade.protocol.TradeProtocol;
import bisq.trade.submarine.SubmarineTrade;

import java.lang.reflect.InvocationTargetException;

public abstract class SubmarineProtocol extends TradeProtocol<SubmarineTrade> {
    private static final String version = "1.0.0";

    public SubmarineProtocol(ServiceProvider serviceProvider, SubmarineTrade model) {
        super(version, serviceProvider, model);
    }

    @Override
    protected EventHandler newEventHandlerFromClass(Class<? extends EventHandler> handlerClass) {
        try {
            return handlerClass.getDeclaredConstructor(ServiceProvider.class, SubmarineTrade.class).newInstance(serviceProvider, model);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void configErrorHandling() {
    }
}
