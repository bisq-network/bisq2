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

package bisq.desktop.main.content.reputation.build_reputation.bond.tab2;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.main.content.reputation.build_reputation.Tab2Controller;
import bisq.desktop.navigation.NavigationTarget;
import bisq.i18n.Res;
import bisq.user.reputation.BondedReputationService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BondedReputationTab2Controller extends Tab2Controller<BondedReputationTab2Model, BondedReputationTab2View, BondScoreSimulation> {
    public BondedReputationTab2Controller(ServiceProvider serviceProvider) {
        super(serviceProvider);
    }

    @Override
    protected BondedReputationTab2Model createModel() {
        return new BondedReputationTab2Model(Res.get("reputation.bond.score.info"),
                String.valueOf(BondedReputationService.WEIGHT),
                Res.get("reputation.bond.formula"));
    }

    @Override
    protected BondScoreSimulation createSimulation() {
        return new BondScoreSimulation();
    }

    @Override
    protected BondedReputationTab2View createView(BondedReputationTab2Model model, BondScoreSimulation simulation) {
        return new BondedReputationTab2View(model, this, simulation.getViewRoot());
    }

    @Override
    public void onBack() {
        Navigation.navigateTo(NavigationTarget.BSQ_BOND_TAB_1);
    }

    @Override
    public void onNext() {
        Navigation.navigateTo(NavigationTarget.BSQ_BOND_TAB_3);
    }
}
