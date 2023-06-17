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

package bisq.desktop.primary.main.content.trade_apps.overview;

import bisq.common.data.Pair;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.i18n.Res;
import bisq.protocol.TradeProtocolAttributes;
import lombok.Getter;

public abstract class TradeOverviewController<M extends TradeOverviewModel> implements Controller {
    @Getter
    protected final M model;

    public TradeOverviewController(M model) {
        this.model = model;

        ProtocolListItem bisqEasy = new ProtocolListItem(TradeProtocolAttributes.Type.BISQ_EASY,
                NavigationTarget.BISQ_EASY,
                Res.get("tradeApps.basic.info.bisqEasy"),
                Res.get("tradeApps.markets.bisqEasy"),
                "",
                "",
                new Pair<>(10000L, 700000L),
                "",
                "",
                "",
                "",
                "",
                "protocol-satoshi-square"
        );
        ProtocolListItem bisqMultisig = new ProtocolListItem(TradeProtocolAttributes.Type.BISQ_MULTISIG,
                NavigationTarget.BISQ_MULTISIG,
                Res.get("tradeApps.basic.info.bisqMultisig"),
                Res.get("tradeApps.markets.bisqMultisig"),
                Res.get("tradeApps.markets.info.bisqMultisig"),
                Res.get("tradeApps.security.info.bisqMultisig"),
                new Pair<>(10000L, 700000L),
                Res.get("tradeApps.privacy.info.bisqMultisig"),
                Res.get("tradeApps.convenience.info.bisqMultisig"),
                Res.get("tradeApps.costs.info.bisqMultisig"),
                Res.get("tradeApps.speed.info.bisqMultisig"),
                "Q4/23",
                "protocol-bisq"
        );
        ProtocolListItem liquidSwap = new ProtocolListItem(TradeProtocolAttributes.Type.LIQUID_SWAP,
                NavigationTarget.LIQUID_SWAP,
                Res.get("tradeApps.basic.info.liquidSwap"),
                Res.get("tradeApps.markets.liquidSwap"),
                Res.get("tradeApps.markets.info.liquidSwap"),
                Res.get("tradeApps.security.info.liquidSwap"),
                new Pair<>(10000L, 700000L),
                Res.get("tradeApps.privacy.info.liquidSwap"),
                Res.get("tradeApps.convenience.info.liquidSwap"),
                Res.get("tradeApps.costs.info.liquidSwap"),
                Res.get("tradeApps.speed.info.liquidSwap"),
                "Q1/24",
                "protocol-liquid"
        );
        ProtocolListItem moneroSwap = new ProtocolListItem(TradeProtocolAttributes.Type.MONERO_SWAP,
                NavigationTarget.MONERO_SWAP,
                Res.get("tradeApps.basic.info.moneroSwap"),
                Res.get("tradeApps.markets.moneroSwap"),
                Res.get("tradeApps.markets.info.moneroSwap"),
                Res.get("tradeApps.security.info.moneroSwap"),
                new Pair<>(10000L, 700000L),
                Res.get("tradeApps.privacy.info.moneroSwap"),
                Res.get("tradeApps.convenience.info.moneroSwap"),
                Res.get("tradeApps.costs.info.moneroSwap"),
                Res.get("tradeApps.speed.info.moneroSwap"),
                "Q1/23",
                "protocol-monero"
        );
        ProtocolListItem lightning = new ProtocolListItem(TradeProtocolAttributes.Type.LIGHTNING_X,
                NavigationTarget.LIGHTNING_X,
                Res.get("tradeApps.basic.info.lightning"),
                Res.get("tradeApps.markets.lightning"),
                Res.get("tradeApps.markets.info.lightning"),
                Res.get("tradeApps.security.info.lightning"),
                new Pair<>(10000L, 700000L),
                Res.get("tradeApps.privacy.info.lightning"),
                Res.get("tradeApps.convenience.info.lightning"),
                Res.get("tradeApps.costs.info.lightning"),
                Res.get("tradeApps.speed.info.lightning"),
                "Q2/23",
                "protocol-lightning"
        );
        ProtocolListItem bsqSwap = new ProtocolListItem(TradeProtocolAttributes.Type.BSQ_SWAP,
                NavigationTarget.BSQ_SWAP,
                Res.get("tradeApps.basic.info.bsqSwap"),
                Res.get("tradeApps.markets.bsqSwap"),
                Res.get("tradeApps.markets.info.bsqSwap"),
                Res.get("tradeApps.security.info.bsqSwap"),
                new Pair<>(10000L, 700000L),
                Res.get("tradeApps.privacy.info.bsqSwap"),
                Res.get("tradeApps.convenience.info.bsqSwap"),
                Res.get("tradeApps.costs.info.bsqSwap"),
                Res.get("tradeApps.speed.info.bsqSwap"),
                "Q2/24",
                "protocol-bsq"
        );
        model.getListItems().setAll(
                bisqEasy,
                bisqMultisig,
                liquidSwap,
               /* moneroSwap,
                lightning,*/
                bsqSwap
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
