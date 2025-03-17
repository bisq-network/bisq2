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

package bisq.desktop.main.content.reputation.build_reputation.accountAge.tab2;

import bisq.bisq_easy.NavigationTarget;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Browser;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AccountAgeTab2Controller implements Controller {
    @Getter
    private final AccountAgeTab2View view;

    public AccountAgeTab2Controller(ServiceProvider serviceProvider) {
        AccountAgeTab2Model model = new AccountAgeTab2Model();
        AccountAgeScoreSimulation simulation = new AccountAgeScoreSimulation();
        view = new AccountAgeTab2View(model, this, simulation.getViewRoot());
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    void onBack() {
        Navigation.navigateTo(NavigationTarget.ACCOUNT_AGE_TAB_1);
    }

    void onNext() {
        Navigation.navigateTo(NavigationTarget.ACCOUNT_AGE_TAB_3);
    }

    void onLearnMore() {
        Browser.open("https://bisq.wiki/Reputation");
    }
}
