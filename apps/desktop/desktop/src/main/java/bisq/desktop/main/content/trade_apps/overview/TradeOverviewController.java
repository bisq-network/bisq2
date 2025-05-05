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
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.main.content.trade_apps.more.MoreProtocolsController;
import bisq.mu_sig.MuSigService;
import bisq.settings.SettingsService;
import lombok.Getter;

import java.util.List;

public class TradeOverviewController implements Controller {
    @Getter
    private final TradeOverviewView view;
    @Getter
    private final TradeOverviewModel model;
    private final SettingsService settingsService;
    private final MuSigService muSigService;
    private Pin isMuSigActivatedPin;

    public TradeOverviewController(ServiceProvider serviceProvider) {
        settingsService = serviceProvider.getSettingsService();
        muSigService = serviceProvider.getMuSigService();

        this.model = new TradeOverviewModel(getMainProtocols(), getMoreProtocols());
        this.view = new TradeOverviewView(model, this);
    }

    @Override
    public void onActivate() {
        isMuSigActivatedPin = FxBindings.bind(model.getIsMuSigActivated())
                .to(muSigService.getMuSigActivated());
    }

    @Override
    public void onDeactivate() {
        isMuSigActivatedPin.unbind();
    }

    void onSelect(ProtocolListItem protocolListItem) {
        NavigationTarget navigationTarget = protocolListItem.getNavigationTarget();
        if (navigationTarget == NavigationTarget.MORE_TRADE_PROTOCOLS) {
            Navigation.navigateTo(navigationTarget, new MoreProtocolsController.InitData(protocolListItem.getTradeProtocolType()));
        } else {
            Navigation.navigateTo(navigationTarget);
        }
    }

    void onActivateMuSig() {
        settingsService.setMuSigActivated(true);
    }

    void onDeactivateMuSig() {
        settingsService.setMuSigActivated(false);
    }

    private List<ProtocolListItem> getMainProtocols() {
        ProtocolListItem bisqEasy = new ProtocolListItem(TradeAppsAttributes.Type.BISQ_EASY,
                NavigationTarget.BISQ_EASY,
                TradeProtocolType.BISQ_EASY,
                new Pair<>(10000L, 700000L),
                ""
        );
        ProtocolListItem muSig = new ProtocolListItem(TradeAppsAttributes.Type.MU_SIG,
                NavigationTarget.MU_SIG_PROTOCOL,
                TradeProtocolType.MU_SIG,
                new Pair<>(10000L, 700000L),
                "Q4/25"
        );
        ProtocolListItem submarine = new ProtocolListItem(TradeAppsAttributes.Type.SUBMARINE,
                NavigationTarget.SUBMARINE,
                TradeProtocolType.SUBMARINE,
                new Pair<>(10000L, 700000L),
                "Q2/26"
        );
        ProtocolListItem liquidFiat = new ProtocolListItem(TradeAppsAttributes.Type.BISQ_LIGHTNING,
                NavigationTarget.BISQ_LIGHTNING,
                TradeProtocolType.BISQ_LIGHTNING,
                new Pair<>(10000L, 700000L),
                "Q4/26"
        );
        return List.of(bisqEasy,
                muSig,
                submarine,
                liquidFiat);
    }

    private List<ProtocolListItem> getMoreProtocols() {
        ProtocolListItem liquidMuSig = new ProtocolListItem(TradeAppsAttributes.Type.LIQUID_MU_SIG,
                NavigationTarget.MORE_TRADE_PROTOCOLS,
                TradeProtocolType.LIQUID_MU_SIG,
                new Pair<>(10000L, 700000L),
                "Q3/26"
        );
        ProtocolListItem moneroSwap = new ProtocolListItem(TradeAppsAttributes.Type.MONERO_SWAP,
                NavigationTarget.MORE_TRADE_PROTOCOLS,
                TradeProtocolType.MONERO_SWAP,
                new Pair<>(10000L, 700000L),
                "Q2/27"
        );
        ProtocolListItem liquidSwap = new ProtocolListItem(TradeAppsAttributes.Type.LIQUID_SWAP,
                NavigationTarget.MORE_TRADE_PROTOCOLS,
                TradeProtocolType.LIQUID_SWAP,
                new Pair<>(10000L, 700000L),
                "Q3/27"
        );
        ProtocolListItem bsqSwap = new ProtocolListItem(TradeAppsAttributes.Type.BSQ_SWAP,
                NavigationTarget.MORE_TRADE_PROTOCOLS,
                TradeProtocolType.BSQ_SWAP,
                new Pair<>(10000L, 700000L),
                "Q4/27"
        );
        return List.of(liquidMuSig,
                moneroSwap,
                liquidSwap,
                bsqSwap);
    }
}
