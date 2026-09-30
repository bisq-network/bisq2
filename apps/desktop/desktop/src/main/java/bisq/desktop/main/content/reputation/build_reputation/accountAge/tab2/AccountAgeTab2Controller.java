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

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.main.content.reputation.build_reputation.Tab2Controller;
import bisq.desktop.navigation.NavigationTarget;
import bisq.i18n.Res;
import bisq.user.reputation.AccountAgeService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AccountAgeTab2Controller extends Tab2Controller<AccountAgeTab2Model, AccountAgeTab2View, AccountAgeScoreSimulation> {
    public AccountAgeTab2Controller(ServiceProvider serviceProvider) {
        super(serviceProvider);
    }

    @Override
    protected AccountAgeTab2Model createModel() {
        return new AccountAgeTab2Model(Res.get("reputation.accountAge.score.info"),
                String.valueOf(AccountAgeService.WEIGHT),
                Res.get("reputation.accountAge.formula"));
    }

    @Override
    protected AccountAgeScoreSimulation createSimulation() {
        return new AccountAgeScoreSimulation();
    }

    @Override
    protected AccountAgeTab2View createView(AccountAgeTab2Model model, AccountAgeScoreSimulation simulation) {
        return new AccountAgeTab2View(model, this, simulation.getViewRoot());
    }

    @Override
    public void onBack() {
        Navigation.navigateTo(NavigationTarget.ACCOUNT_AGE_TAB_1);
    }

    @Override
    public void onNext() {
        Navigation.navigateTo(NavigationTarget.ACCOUNT_AGE_TAB_3);
    }
}
