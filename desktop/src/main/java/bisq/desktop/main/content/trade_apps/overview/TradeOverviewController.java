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

package bisq.desktop.main.content.trade_apps.overview;

import bisq.common.data.Pair;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import lombok.Getter;

public abstract class TradeOverviewController<M extends TradeOverviewModel> implements Controller {
    @Getter
    protected final M model;

    public TradeOverviewController(M model) {
        this.model = model;

        ProtocolListItem bisqEasy = new ProtocolListItem(TradeAppsAttributes.Type.BISQ_EASY,
                NavigationTarget.BISQ_EASY,
                new Pair<>(10000L, 700000L),
                ""
        );
        ProtocolListItem multisig = new ProtocolListItem(TradeAppsAttributes.Type.MULTISIG,
                NavigationTarget.MULTISIG,
                new Pair<>(10000L, 700000L),
                "Q4/23"
        );
        ProtocolListItem submarine = new ProtocolListItem(TradeAppsAttributes.Type.SUBMARINE,
                NavigationTarget.SUBMARINE,
                new Pair<>(10000L, 700000L),
                "Q1/24"
        );
        ProtocolListItem liquidMultisig = new ProtocolListItem(TradeAppsAttributes.Type.LIQUID_MULTISIG,
                NavigationTarget.LIQUID_MULTISIG,
                new Pair<>(10000L, 700000L),
                "Q2/24"
        );
        ProtocolListItem liquidFiat = new ProtocolListItem(TradeAppsAttributes.Type.LIGHTNING_FIAT,
                NavigationTarget.LIGHTNING_FIAT,
                new Pair<>(10000L, 700000L),
                "Q3/24"
        );
        ProtocolListItem lightningEscrow = new ProtocolListItem(TradeAppsAttributes.Type.LIGHTNING_ESCROW,
                NavigationTarget.LIGHTNING_ESCROW,
                new Pair<>(10000L, 700000L),
                "Q4/24"
        );
        ProtocolListItem moneroSwap = new ProtocolListItem(TradeAppsAttributes.Type.MONERO_SWAP,
                NavigationTarget.MONERO_SWAP,
                new Pair<>(10000L, 700000L),
                "Q4/24"
        );
        ProtocolListItem liquidSwap = new ProtocolListItem(TradeAppsAttributes.Type.LIQUID_SWAP,
                NavigationTarget.LIQUID_SWAP,
                new Pair<>(10000L, 700000L),
                "Q4/24"
        );
        ProtocolListItem bsqSwap = new ProtocolListItem(TradeAppsAttributes.Type.BSQ_SWAP,
                NavigationTarget.BSQ_SWAP,
                new Pair<>(10000L, 700000L),
                "Q4/24"
        );

        model.getListItems().setAll(
                bisqEasy,
                multisig,
                submarine,
                liquidMultisig,
                liquidFiat,
                lightningEscrow,
                moneroSwap,
                liquidSwap,
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
