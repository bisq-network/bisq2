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

package bisq.desktop.main.content.mu_sig.onboarding;

import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.main.content.mu_sig.onboarding.top_panel.MuSigDashboardTopPanel;
import bisq.desktop.navigation.NavigationTarget;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MuSigDashboardController implements Controller {
    @Getter
    private final MuSigDashboardView view;
    private Pin cookieChangedPin;

    public MuSigDashboardController(ServiceProvider serviceProvider) {
        MuSigDashboardTopPanel muSigDashboardTopPanel = new MuSigDashboardTopPanel(serviceProvider);
        MuSigDashboardModel model = new MuSigDashboardModel();
        view = new MuSigDashboardView(model, this, muSigDashboardTopPanel.getViewRoot());
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    void onOpenTradeGuide() {
        Navigation.navigateTo(NavigationTarget.MU_SIG_GUIDE);
    }

    void onExploreAmountLimit() {
        Navigation.navigateTo(NavigationTarget.MU_SIG_TRADE_LIMITS);
    }
}
