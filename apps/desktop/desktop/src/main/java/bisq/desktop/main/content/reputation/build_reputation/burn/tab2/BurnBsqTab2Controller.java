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

package bisq.desktop.main.content.reputation.build_reputation.burn.tab2;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.main.content.reputation.build_reputation.Tab2Controller;
import bisq.desktop.navigation.NavigationTarget;
import bisq.i18n.Res;
import bisq.user.reputation.ProofOfBurnService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BurnBsqTab2Controller extends Tab2Controller<BurnBsqTab2Model, BurnBsqTab2View, BurnBsqScoreSimulation> {
    public BurnBsqTab2Controller(ServiceProvider serviceProvider) {
        super(serviceProvider);
    }

    @Override
    protected BurnBsqTab2Model createModel() {
        return new BurnBsqTab2Model(Res.get("reputation.burnedBsq.score.info"),
                String.valueOf(ProofOfBurnService.WEIGHT),
                Res.get("reputation.burnedBsq.formula"));
    }

    @Override
    protected BurnBsqScoreSimulation createSimulation() {
        return new BurnBsqScoreSimulation();
    }

    @Override
    protected BurnBsqTab2View createView(BurnBsqTab2Model model, BurnBsqScoreSimulation simulation) {
        return new BurnBsqTab2View(model, this, simulation.getViewRoot());
    }

    @Override
    public void onBack() {
        Navigation.navigateTo(NavigationTarget.BURN_BSQ_TAB_1);
    }

    @Override
    public void onNext() {
        Navigation.navigateTo(NavigationTarget.BURN_BSQ_TAB_3);
    }
}
