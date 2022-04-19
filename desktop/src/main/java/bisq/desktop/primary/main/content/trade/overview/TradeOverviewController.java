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
import bisq.i18n.Res;
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
                        Res.get("trade.protocols.markets.SATOSHI_SQUARE"),
                        Res.get("trade.protocols.markets.info.SATOSHI_SQUARE"),
                        Res.get("trade.protocols.security.info.SATOSHI_SQUARE"),
                        new Pair<>(10000L, 700000L),
                        Res.get("trade.protocols.privacy.info.SATOSHI_SQUARE"),
                        Res.get("trade.protocols.convenience.info.SATOSHI_SQUARE"),
                        Res.get("trade.protocols.costs.info.SATOSHI_SQUARE"),
                        Res.get("trade.protocols.speed.info.SATOSHI_SQUARE"),
                        ""
                ),
                new ProtocolListItem(SwapProtocol.Type.LIQUID_SWAP,
                        NavigationTarget.LIQUID_SWAP,
                        Res.get("trade.protocols.markets.LIQUID_SWAP"),
                        Res.get("trade.protocols.markets.info.LIQUID_SWAP"),
                        Res.get("trade.protocols.security.info.LIQUID_SWAP"),
                        new Pair<>(10000L, 700000L),
                        Res.get("trade.protocols.privacy.info.LIQUID_SWAP"),
                        Res.get("trade.protocols.convenience.info.LIQUID_SWAP"),
                        Res.get("trade.protocols.costs.info.LIQUID_SWAP"),
                        Res.get("trade.protocols.speed.info.LIQUID_SWAP"),
                        "Q2/22"
                ),
                new ProtocolListItem(SwapProtocol.Type.ATOMIC_CROSS_CHAIN_SWAP,
                        NavigationTarget.ATOMIC_CROSS_CHAIN_SWAP,
                        Res.get("trade.protocols.markets.ATOMIC_CROSS_CHAIN_SWAP"),
                        Res.get("trade.protocols.markets.info.ATOMIC_CROSS_CHAIN_SWAP"),
                        Res.get("trade.protocols.security.info.ATOMIC_CROSS_CHAIN_SWAP"),
                        new Pair<>(10000L, 700000L),
                        Res.get("trade.protocols.privacy.info.ATOMIC_CROSS_CHAIN_SWAP"),
                        Res.get("trade.protocols.convenience.info.ATOMIC_CROSS_CHAIN_SWAP"),
                        Res.get("trade.protocols.costs.info.ATOMIC_CROSS_CHAIN_SWAP"),
                        Res.get("trade.protocols.speed.info.ATOMIC_CROSS_CHAIN_SWAP"),
                        "Q3/22"
                ),
                new ProtocolListItem(SwapProtocol.Type.BISQ_MULTI_SIG,
                        NavigationTarget.BISQ_MULTI_SIG,
                        Res.get("trade.protocols.markets.BISQ_MULTI_SIG"),
                        Res.get("trade.protocols.markets.info.BISQ_MULTI_SIG"),
                        Res.get("trade.protocols.security.info.BISQ_MULTI_SIG"),
                        new Pair<>(10000L, 700000L),
                        Res.get("trade.protocols.privacy.info.BISQ_MULTI_SIG"),
                        Res.get("trade.protocols.convenience.info.BISQ_MULTI_SIG"),
                        Res.get("trade.protocols.costs.info.BISQ_MULTI_SIG"),
                        Res.get("trade.protocols.speed.info.BISQ_MULTI_SIG"),
                        "Q3/22"
                ),
                new ProtocolListItem(SwapProtocol.Type.BSQ_SWAP,
                        NavigationTarget.BSQ_SWAP,
                        Res.get("trade.protocols.markets.BSQ_SWAP"),
                        Res.get("trade.protocols.markets.info.BSQ_SWAP"),
                        Res.get("trade.protocols.security.info.BSQ_SWAP"),
                        new Pair<>(10000L, 700000L),
                        Res.get("trade.protocols.privacy.info.BSQ_SWAP"),
                        Res.get("trade.protocols.convenience.info.BSQ_SWAP"),
                        Res.get("trade.protocols.costs.info.BSQ_SWAP"),
                        Res.get("trade.protocols.speed.info.BSQ_SWAP"),
                        "Q4/22"
                ),
                new ProtocolListItem(SwapProtocol.Type.LN_3_PARTY,
                        NavigationTarget.LN_3_PARTY,
                        Res.get("trade.protocols.markets.LN_3_PARTY"),
                        Res.get("trade.protocols.markets.info.LN_3_PARTY"),
                        Res.get("trade.protocols.security.info.LN_3_PARTY"),
                        new Pair<>(10000L, 700000L),
                        Res.get("trade.protocols.privacy.info.LN_3_PARTY"),
                        Res.get("trade.protocols.convenience.info.LN_3_PARTY"),
                        Res.get("trade.protocols.costs.info.LN_3_PARTY"),
                        Res.get("trade.protocols.speed.info.LN_3_PARTY"),
                        "Q4/22"
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
