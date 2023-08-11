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

import bisq.account.protocol_type.TradeProtocolType;
import bisq.common.data.Pair;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.main.content.trade_apps.more.MoreProtocolsController;
import lombok.Getter;

import java.util.List;

public class TradeOverviewController implements Controller {
    @Getter
    private final TradeOverviewView view;
    @Getter
    private final TradeOverviewModel model;

    public TradeOverviewController(ServiceProvider serviceProvider) {
        this.model = new TradeOverviewModel(getMainProtocols(), getMoreProtocols());
        this.view = new TradeOverviewView(model, this);
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    void onSelect(ProtocolListItem protocolListItem) {
        NavigationTarget navigationTarget = protocolListItem.getNavigationTarget();
        if (navigationTarget == NavigationTarget.MORE_TRADE_PROTOCOLS) {
            Navigation.navigateTo(navigationTarget, new MoreProtocolsController.InitData(protocolListItem.getTradeProtocolType()));
        } else {
            Navigation.navigateTo(navigationTarget);
        }
    }

    private List<ProtocolListItem> getMainProtocols() {
        ProtocolListItem bisqEasy = new ProtocolListItem(TradeAppsAttributes.Type.BISQ_EASY,
                NavigationTarget.BISQ_EASY,
                TradeProtocolType.BISQ_EASY,
                new Pair<>(10000L, 700000L),
                ""
        );
        ProtocolListItem multisig = new ProtocolListItem(TradeAppsAttributes.Type.MULTISIG,
                NavigationTarget.MULTISIG,
                TradeProtocolType.MULTISIG,
                new Pair<>(10000L, 700000L),
                "Q4/23"
        );
        ProtocolListItem submarine = new ProtocolListItem(TradeAppsAttributes.Type.SUBMARINE,
                NavigationTarget.SUBMARINE,
                TradeProtocolType.SUBMARINE,
                new Pair<>(10000L, 700000L),
                "Q1/24"
        );
        ProtocolListItem liquidFiat = new ProtocolListItem(TradeAppsAttributes.Type.LIGHTNING_FIAT,
                NavigationTarget.LIGHTNING_FIAT,
                TradeProtocolType.LIGHTNING_FIAT,
                new Pair<>(10000L, 700000L),
                "Q3/24"
        );
        return List.of(bisqEasy,
                multisig,
                submarine,
                liquidFiat);
    }

    private List<ProtocolListItem> getMoreProtocols() {
        ProtocolListItem liquidMultisig = new ProtocolListItem(TradeAppsAttributes.Type.LIQUID_MULTISIG,
                NavigationTarget.MORE_TRADE_PROTOCOLS,
                TradeProtocolType.LIQUID_MULTISIG,
                new Pair<>(10000L, 700000L),
                "Q2/24"
        );
        ProtocolListItem lightningEscrow = new ProtocolListItem(TradeAppsAttributes.Type.LIGHTNING_ESCROW,
                NavigationTarget.MORE_TRADE_PROTOCOLS,
                TradeProtocolType.LIGHTNING_ESCROW,
                new Pair<>(10000L, 700000L),
                "Q4/24"
        );
        ProtocolListItem moneroSwap = new ProtocolListItem(TradeAppsAttributes.Type.MONERO_SWAP,
                NavigationTarget.MORE_TRADE_PROTOCOLS,
                TradeProtocolType.MONERO_SWAP,
                new Pair<>(10000L, 700000L),
                "Q4/24"
        );
        ProtocolListItem liquidSwap = new ProtocolListItem(TradeAppsAttributes.Type.LIQUID_SWAP,
                NavigationTarget.MORE_TRADE_PROTOCOLS,
                TradeProtocolType.LIQUID_SWAP,
                new Pair<>(10000L, 700000L),
                "Q4/24"
        );
        ProtocolListItem bsqSwap = new ProtocolListItem(TradeAppsAttributes.Type.BSQ_SWAP,
                NavigationTarget.MORE_TRADE_PROTOCOLS,
                TradeProtocolType.BSQ_SWAP,
                new Pair<>(10000L, 700000L),
                "Q4/24"
        );
        return List.of(liquidMultisig,
                lightningEscrow,
                moneroSwap,
                liquidSwap,
                bsqSwap);
    }
}
