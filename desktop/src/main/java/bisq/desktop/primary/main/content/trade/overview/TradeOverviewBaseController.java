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

import bisq.common.data.Pair;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.i18n.Res;
import bisq.protocol.SwapProtocol;
import lombok.Getter;

public abstract class TradeOverviewBaseController<M extends TradeOverviewBaseModel> implements Controller {
    @Getter
    protected final M model;

    public TradeOverviewBaseController(M model) {
        this.model = model;

        model.getListItems().setAll(
                new ProtocolListItem(SwapProtocol.Type.BISQ_EASY,
                        NavigationTarget.BISQ_EASY,
                        Res.get("trade.protocols.basic.info.SATOSHI_SQUARE"),
                        Res.get("trade.protocols.markets.SATOSHI_SQUARE"),
                        Res.get("trade.protocols.markets.info.SATOSHI_SQUARE"),
                        Res.get("trade.protocols.security.info.SATOSHI_SQUARE"),
                        new Pair<>(10000L, 700000L),
                        Res.get("trade.protocols.privacy.info.SATOSHI_SQUARE"),
                        Res.get("trade.protocols.convenience.info.SATOSHI_SQUARE"),
                        Res.get("trade.protocols.costs.info.SATOSHI_SQUARE"),
                        Res.get("trade.protocols.speed.info.SATOSHI_SQUARE"),
                        "",
                        "protocol-satoshi-square"
                ),
                new ProtocolListItem(SwapProtocol.Type.LIQUID_SWAP,
                        NavigationTarget.LIQUID_SWAP,
                        Res.get("trade.protocols.basic.info.LIQUID_SWAP"),
                        Res.get("trade.protocols.markets.LIQUID_SWAP"),
                        Res.get("trade.protocols.markets.info.LIQUID_SWAP"),
                        Res.get("trade.protocols.security.info.LIQUID_SWAP"),
                        new Pair<>(10000L, 700000L),
                        Res.get("trade.protocols.privacy.info.LIQUID_SWAP"),
                        Res.get("trade.protocols.convenience.info.LIQUID_SWAP"),
                        Res.get("trade.protocols.costs.info.LIQUID_SWAP"),
                        Res.get("trade.protocols.speed.info.LIQUID_SWAP"),
                        "Q2/22",
                        "protocol-liquid"
                ),
                new ProtocolListItem(SwapProtocol.Type.MONERO_SWAP,
                        NavigationTarget.MONERO_SWAP,
                        Res.get("trade.protocols.basic.info.MONERO_SWAP"),
                        Res.get("trade.protocols.markets.MONERO_SWAP"),
                        Res.get("trade.protocols.markets.info.MONERO_SWAP"),
                        Res.get("trade.protocols.security.info.MONERO_SWAP"),
                        new Pair<>(10000L, 700000L),
                        Res.get("trade.protocols.privacy.info.MONERO_SWAP"),
                        Res.get("trade.protocols.convenience.info.MONERO_SWAP"),
                        Res.get("trade.protocols.costs.info.MONERO_SWAP"),
                        Res.get("trade.protocols.speed.info.MONERO_SWAP"),
                        "Q3/22",
                        "protocol-monero"
                ),
                new ProtocolListItem(SwapProtocol.Type.BISQ_MULTISIG,
                        NavigationTarget.BISQ_MULTISIG,
                        Res.get("trade.protocols.basic.info.BISQ_MULTISIG"),
                        Res.get("trade.protocols.markets.BISQ_MULTISIG"),
                        Res.get("trade.protocols.markets.info.BISQ_MULTISIG"),
                        Res.get("trade.protocols.security.info.BISQ_MULTISIG"),
                        new Pair<>(10000L, 700000L),
                        Res.get("trade.protocols.privacy.info.BISQ_MULTISIG"),
                        Res.get("trade.protocols.convenience.info.BISQ_MULTISIG"),
                        Res.get("trade.protocols.costs.info.BISQ_MULTISIG"),
                        Res.get("trade.protocols.speed.info.BISQ_MULTISIG"),
                        "Q3/22",
                        "protocol-bisq"
                ),
                new ProtocolListItem(SwapProtocol.Type.BSQ_SWAP,
                        NavigationTarget.BSQ_SWAP,
                        Res.get("trade.protocols.basic.info.BSQ_SWAP"),
                        Res.get("trade.protocols.markets.BSQ_SWAP"),
                        Res.get("trade.protocols.markets.info.BSQ_SWAP"),
                        Res.get("trade.protocols.security.info.BSQ_SWAP"),
                        new Pair<>(10000L, 700000L),
                        Res.get("trade.protocols.privacy.info.BSQ_SWAP"),
                        Res.get("trade.protocols.convenience.info.BSQ_SWAP"),
                        Res.get("trade.protocols.costs.info.BSQ_SWAP"),
                        Res.get("trade.protocols.speed.info.BSQ_SWAP"),
                        "Q4/22",
                        "protocol-bsq"
                ),
                new ProtocolListItem(SwapProtocol.Type.LIGHTNING_X,
                        NavigationTarget.LIGHTNING_X,
                        Res.get("trade.protocols.basic.info.LIGHTNING_X"),
                        Res.get("trade.protocols.markets.LIGHTNING_X"),
                        Res.get("trade.protocols.markets.info.LIGHTNING_X"),
                        Res.get("trade.protocols.security.info.LIGHTNING_X"),
                        new Pair<>(10000L, 700000L),
                        Res.get("trade.protocols.privacy.info.LIGHTNING_X"),
                        Res.get("trade.protocols.convenience.info.LIGHTNING_X"),
                        Res.get("trade.protocols.costs.info.LIGHTNING_X"),
                        Res.get("trade.protocols.speed.info.LIGHTNING_X"),
                        "Q4/22",
                        "protocol-lightning"
                )
        );
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    public void onSelect(ProtocolListItem protocolListItem) {
        Navigation.navigateTo(protocolListItem.getNavigationTarget());
    }
}
