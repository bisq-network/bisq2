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

package bisq.desktop.main.content.bisq_easy.open_trades.trade_state.states;

import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.desktop.ServiceProvider;
import bisq.trade.bisq_easy.BisqEasyTrade;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SellerStateMainChain3b extends StateMainChain3b<SellerStateMainChain3b.Controller> {
    public SellerStateMainChain3b(ServiceProvider serviceProvider,
                                  BisqEasyTrade bisqEasyTrade,
                                  BisqEasyOpenTradeChannel channel) {
        super(serviceProvider, bisqEasyTrade, channel);
    }

    public View getView() {
        return controller.getView();
    }

    @Override
    protected Controller getController(ServiceProvider serviceProvider,
                                       BisqEasyTrade bisqEasyTrade,
                                       BisqEasyOpenTradeChannel channel) {
        return new Controller(serviceProvider, bisqEasyTrade, channel);
    }

    protected static class Controller extends StateMainChain3b.Controller<Model, View> {
        protected Controller(ServiceProvider serviceProvider,
                             BisqEasyTrade bisqEasyTrade,
                             BisqEasyOpenTradeChannel channel) {
            super(serviceProvider, bisqEasyTrade, channel);
        }

        @Override
        protected Model createModel(BisqEasyTrade bisqEasyTrade, BisqEasyOpenTradeChannel channel) {
            return new Model(bisqEasyTrade, channel);
        }

        @Override
        protected View createView() {
            return new View(model, this);
        }
    }

    @Getter
    protected static class Model extends StateMainChain3b.Model {
        protected Model(BisqEasyTrade bisqEasyTrade, BisqEasyOpenTradeChannel channel) {
            super(bisqEasyTrade, channel);
        }
    }

    public static class View extends StateMainChain3b.View<Model, Controller> {
        protected View(Model model, Controller controller) {
            super(model, controller);
        }
    }
}
