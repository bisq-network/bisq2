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
import bisq.user.reputation.SignedWitnessService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class SignedWitnessTab2Controller implements Controller {
    @Getter
    private final SignedWitnessTab2View view;

    private final SignedWitnessTab2Model model;

    private Subscription agePin, ageAsStringPin, scorePin;

    public SignedWitnessTab2Controller(ServiceProvider serviceProvider) {
        model = new SignedWitnessTab2Model();
        view = new SignedWitnessTab2View(model, this);

        model.getAge().set(0);
        model.getAgeAsString().set("0");
    }

    @Override
    public void onActivate() {
        agePin = EasyBind.subscribe(model.getAge(), age -> model.getAgeAsString().set(String.valueOf(age)));
        ageAsStringPin = EasyBind.subscribe(model.getAgeAsString(), ageAsString -> {
            try {
                model.getAge().set(Integer.parseInt(ageAsString));
            } catch (Exception e) {
                log.error("A failure occurred while setting the age string value from the age pin.");
            }
        });

        scorePin = EasyBind.subscribe(model.getAge(), age -> model.getScore().set(calculateSimScore(age)));
    }

    @Override
    public void onDeactivate() {
        agePin.unsubscribe();
        ageAsStringPin.unsubscribe();
        scorePin.unsubscribe();
    }

    private String calculateSimScore(Number age) {
        try {
            long ageInDays = age.intValue();
            long totalScore = SignedWitnessService.doCalculateScore(ageInDays);
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
