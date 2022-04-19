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

package bisq.desktop.primary.main.content.trade.overview;

import bisq.application.DefaultApplicationService;
import bisq.common.data.Pair;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.protocol.SwapProtocol;
import lombok.Getter;

public class TradeOverviewController implements Controller {
    @Getter
    private final TradeOverviewModel model;
    @Getter
    private final TradeOverviewView view;

    public TradeOverviewController(DefaultApplicationService applicationService) {
        model = new TradeOverviewModel();
        view = new TradeOverviewView(model, this);
    }

    @Override
    public void onActivate() {
        model.getListItems().addAll(
                new ProtocolListItem(SwapProtocol.Type.SATOSHI_SQUARE,
                        NavigationTarget.SATOSHI_SQUARE,
                        "Any supported Bisq market",
                        "Any supported Bisq market",
                        "Sellers earn reputation by burning BSQ",
                        new Pair<>(10000L, 700000L),
                        "0.0001 BTC - 0.007 BTC",
                        "Depending on Fiat transfer method used. Online Fiat transfer usually reveals identity to trade peer.",
                        "Very easy to use and no entrance barriers.",
                        "Seller need to burn BSQ for gaining reputation. Those costs will be transmitted to the buyer in form of a price premium. Also usaully small amounts have higher premium.",
                        "Depending on peers online presence and time it takes to send Fiat currency."
                ),
                new ProtocolListItem(SwapProtocol.Type.LIQUID_SWAP,
                        NavigationTarget.LIQUID_SWAPS,
                        "Any supported asset pair on Liquid (e.g. L-BTC/USDT)",
                        "Any supported asset pair on Liquid (e.g. L-BTC/USDT)",
                        "Smart contract",
                        new Pair<>(10000L, 200000000L),
                        "0.0001 BTC - 2 L-BTC",
                        "Liquid supports confidential transactions, thus not revealing the amount on the blockchain.",
                        "User need to install the Liquid wallet Elements.",
                        "Transactions fees are very low on Liquid.",
                        "Blockchain confirmation time is about 1 min. on Liquid."
                ),
                new ProtocolListItem(SwapProtocol.Type.ATOMIC_CROSS_CHAIN_SWAP,
                        NavigationTarget.XMR_SWAPS,
                        "Any supported asset pair on Liquid (e.g. L-BTC/USDT)",
                        "Any supported asset pair on Liquid (e.g. L-BTC/USDT)",
                        "Smart contract",
                        new Pair<>(10000L, 200000000L),
                        "0.0001 BTC - 2 L-BTC",
                        "Liquid supports confidential transactions, thus not revealing the amount on the blockchain.",
                        "User need to install the Liquid wallet Elements.",
                        "Transactions fees are very low on Liquid.",
                        "Blockchain confirmation time is about 1 min. on Liquid."
                ),
                new ProtocolListItem(SwapProtocol.Type.BISQ_MULTI_SIG,
                        NavigationTarget.MULTI_SIG,
                        "Any supported asset pair on Liquid (e.g. L-BTC/USDT)",
                        "Any supported asset pair on Liquid (e.g. L-BTC/USDT)",
                        "Smart contract",
                        new Pair<>(10000L, 200000000L),
                        "0.0001 BTC - 2 L-BTC",
                        "Liquid supports confidential transactions, thus not revealing the amount on the blockchain.",
                        "User need to install the Liquid wallet Elements.",
                        "Transactions fees are very low on Liquid.",
                        "Blockchain confirmation time is about 1 min. on Liquid."
                )
        );
    }

    @Override
    public void onDeactivate() {
    }

    public void onSelect(ProtocolListItem protocolListItem) {
        Navigation.navigateTo(protocolListItem.getNavigationTarget());
    }
}
