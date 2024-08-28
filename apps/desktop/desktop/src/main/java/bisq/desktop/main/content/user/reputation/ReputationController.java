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

package bisq.desktop.main.content.user.reputation;

import bisq.bisq_easy.NavigationTarget;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Browser;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.main.content.user.reputation.list.ReputationListController;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ReputationController implements Controller {
    @Getter
    private final ReputationView view;

    public ReputationController(ServiceProvider serviceProvider) {
        ReputationListController reputationListController = new ReputationListController(serviceProvider);

        ReputationModel model = new ReputationModel();
        view = new ReputationView(model, this, reputationListController.getView().getRoot());
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    public void onBurnBsq() {
        Navigation.navigateTo(NavigationTarget.BURN_BSQ);
    }

    public void onBsqBond() {
        Navigation.navigateTo(NavigationTarget.BSQ_BOND);
    }

    public void onAccountAge() {
        Navigation.navigateTo(NavigationTarget.ACCOUNT_AGE);
    }

    public void onSignedAccount() {
        Navigation.navigateTo(NavigationTarget.SIGNED_WITNESS);
    }

    public void onLearnMore() {
        Browser.open("https://bisq.wiki/Reputation");
    }
}
