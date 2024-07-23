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
import bisq.bisq_easy.NavigationTarget;
import bisq.common.data.Pair;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
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
        ProtocolListItem bisqMuSig = new ProtocolListItem(TradeAppsAttributes.Type.BISQ_MU_SIG,
                NavigationTarget.BISQ_MU_SIG,
                TradeProtocolType.BISQ_MU_SIG,
                new Pair<>(10000L, 700000L),
                "Q4/24"
        );
        ProtocolListItem submarine = new ProtocolListItem(TradeAppsAttributes.Type.SUBMARINE,
                NavigationTarget.SUBMARINE,
                TradeProtocolType.SUBMARINE,
                new Pair<>(10000L, 700000L),
                "Q2/25"
        );
        ProtocolListItem liquidFiat = new ProtocolListItem(TradeAppsAttributes.Type.BISQ_LIGHTNING,
                NavigationTarget.BISQ_LIGHTNING,
                TradeProtocolType.BISQ_LIGHTNING,
                new Pair<>(10000L, 700000L),
                "Q2/25"
        );
        return List.of(bisqEasy,
                bisqMuSig,
                submarine,
                liquidFiat);
    }

    private List<ProtocolListItem> getMoreProtocols() {
        ProtocolListItem liquidMuSig = new ProtocolListItem(TradeAppsAttributes.Type.LIQUID_MU_SIG,
                NavigationTarget.MORE_TRADE_PROTOCOLS,
                TradeProtocolType.LIQUID_MU_SIG,
                new Pair<>(10000L, 700000L),
                "Q2/25"
        );
        ProtocolListItem moneroSwap = new ProtocolListItem(TradeAppsAttributes.Type.MONERO_SWAP,
                NavigationTarget.MORE_TRADE_PROTOCOLS,
                TradeProtocolType.MONERO_SWAP,
                new Pair<>(10000L, 700000L),
                "Q3/25"
        );
        ProtocolListItem liquidSwap = new ProtocolListItem(TradeAppsAttributes.Type.LIQUID_SWAP,
                NavigationTarget.MORE_TRADE_PROTOCOLS,
                TradeProtocolType.LIQUID_SWAP,
                new Pair<>(10000L, 700000L),
                "Q2/25"
        );
        ProtocolListItem bsqSwap = new ProtocolListItem(TradeAppsAttributes.Type.BSQ_SWAP,
                NavigationTarget.MORE_TRADE_PROTOCOLS,
                TradeProtocolType.BSQ_SWAP,
                new Pair<>(10000L, 700000L),
                "Q3/25"
        );
        return List.of(liquidMuSig,
                moneroSwap,
                liquidSwap,
                bsqSwap);
    }
}
