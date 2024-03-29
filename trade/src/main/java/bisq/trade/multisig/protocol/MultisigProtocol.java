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

package bisq.trade.multisig.protocol;

import bisq.common.fsm.EventHandler;
import bisq.trade.ServiceProvider;
import bisq.trade.multisig.MultisigTrade;
import bisq.trade.protocol.TradeProtocol;

import java.lang.reflect.InvocationTargetException;

public abstract class MultisigProtocol extends TradeProtocol<MultisigTrade> {
    private static final String version = "1.0.0";

    public MultisigProtocol(ServiceProvider serviceProvider, MultisigTrade model) {
        super(version, serviceProvider, model);
    }

    @Override
    protected EventHandler newEventHandlerFromClass(Class<? extends EventHandler> handlerClass) {
        try {
            return handlerClass.getDeclaredConstructor(ServiceProvider.class, MultisigTrade.class).newInstance(serviceProvider, model);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void configErrorHandling() {
    }
}
