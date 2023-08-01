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

package bisq.desktop.main.content.user.reputation.signedAccount.tab2;

import bisq.common.util.MathUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Browser;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.user.reputation.ProofOfBurnService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SignedWitnessTab2Controller implements Controller {
    @Getter
    private final SignedWitnessTab2View view;

    public SignedWitnessTab2Controller(ServiceProvider serviceProvider) {
        SignedWitnessTab2Model model = new SignedWitnessTab2Model();
        SignedWitnessScoreSimulation simulation = new SignedWitnessScoreSimulation();
        view = new SignedWitnessTab2View(model, this, simulation.getViewRoot());
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    private static String calculateSimScore(String amount, Number age) {
        try {
            // amountAsLong is the smallest unit of BSQ (100 = 1 BSQ)
            long amountAsLong = MathUtils.roundDoubleToLong(Double.parseDouble(amount) * 100);
            long ageInDays = age.intValue();
            long totalScore = ProofOfBurnService.doCalculateScore(amountAsLong, ageInDays);
            return String.valueOf(totalScore);
        } catch (Exception e) {
            return "";
        }
    }

    void onBack() {
        Navigation.navigateTo(NavigationTarget.SIGNED_WITNESS_TAB_1);
    }

    void onNext() {
        Navigation.navigateTo(NavigationTarget.SIGNED_WITNESS_TAB_3);
    }

    void onLearnMore() {
        Browser.open("https://bisq.wiki/reputation/signedAccount");
    }
}
