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

package bisq.desktop.main.content.reputation.build_reputation.signedAccount.tab2;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.main.content.reputation.build_reputation.Tab2Controller;
import bisq.desktop.navigation.NavigationTarget;
import bisq.i18n.Res;
import bisq.user.reputation.SignedWitnessService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SignedWitnessTab2Controller extends Tab2Controller<SignedWitnessTab2Model, SignedWitnessTab2View, SignedWitnessScoreSimulation> {
    public SignedWitnessTab2Controller(ServiceProvider serviceProvider) {
        super(serviceProvider);
    }

    @Override
    protected SignedWitnessTab2Model createModel() {
        return new SignedWitnessTab2Model(Res.get("reputation.signedWitness.score.info"),
                String.valueOf(SignedWitnessService.WEIGHT),
                Res.get("reputation.signedWitness.formula"));
    }

    @Override
    protected SignedWitnessScoreSimulation createSimulation() {
        return new SignedWitnessScoreSimulation();
    }

    @Override
    protected SignedWitnessTab2View createView(SignedWitnessTab2Model model, SignedWitnessScoreSimulation simulation) {
        return new SignedWitnessTab2View(model, this, simulation.getViewRoot());
    }

    @Override
    public void onBack() {
        Navigation.navigateTo(NavigationTarget.SIGNED_WITNESS_TAB_1);
    }

    @Override
    public void onNext() {
        Navigation.navigateTo(NavigationTarget.SIGNED_WITNESS_TAB_3);
    }
}
