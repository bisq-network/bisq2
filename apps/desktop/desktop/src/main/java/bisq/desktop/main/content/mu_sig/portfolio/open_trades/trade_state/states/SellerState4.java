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

package bisq.desktop.main.content.mu_sig.portfolio.open_trades.trade_state.states;

import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannel;
import bisq.desktop.ServiceProvider;
import bisq.trade.mu_sig.MuSigTrade;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SellerState4 extends State4<SellerState4.Controller> {
    public SellerState4(ServiceProvider serviceProvider,
                        MuSigTrade trade,
                        MuSigOpenTradeChannel channel) {
        super(serviceProvider, trade, channel);
    }

    @Override
    protected Controller getController(ServiceProvider serviceProvider,
                                       MuSigTrade trade,
                                       MuSigOpenTradeChannel channel) {
        return new Controller(serviceProvider, trade, channel);
    }

    public View getView() {
        return controller.getView();
    }

    protected static class Controller extends State4.Controller<Model, View> {
        protected Controller(ServiceProvider serviceProvider,
                             MuSigTrade trade,
                             MuSigOpenTradeChannel channel) {
            super(serviceProvider, trade, channel);
        }

        @Override
        protected Model createModel(MuSigTrade trade, MuSigOpenTradeChannel channel) {
            return new Model(trade, channel);
        }

        @Override
        protected View createView() {
            return new View(model, this);
        }
    }

    @Setter
    @Getter
    protected static class Model extends State4.Model {
        protected Model(MuSigTrade trade, MuSigOpenTradeChannel channel) {
            super(trade, channel);
        }
    }

    public static class View extends State4.View<Model, Controller> {
        protected View(Model model, Controller controller) {
            super(model, controller);
        }
    }
}
