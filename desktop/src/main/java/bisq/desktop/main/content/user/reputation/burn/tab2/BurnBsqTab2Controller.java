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

package bisq.desktop.main.content.user.reputation.burn.tab2;

import bisq.common.util.MathUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Browser;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.user.reputation.ProofOfBurnService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;
import org.fxmisc.easybind.monadic.MonadicBinding;

@Slf4j
public class BurnBsqTab2Controller implements Controller {
    @Getter
    private final BurnBsqTab2View view;

    private final BurnBsqTab2Model model;

    private Subscription agePin, ageAsStringPin, scorePin;

    public BurnBsqTab2Controller(ServiceProvider serviceProvider) {
        model = new BurnBsqTab2Model();
        view = new BurnBsqTab2View(model, this);

        model.getAmount().set("100");
        model.getAge().set(0);
        model.getAgeAsString().set("0");
    }

    @Override
    public void onActivate() {
        agePin = EasyBind.subscribe(model.getAge(),
                    age -> model.getAgeAsString().set(String.valueOf(age)));
        ageAsStringPin = EasyBind.subscribe(model.getAgeAsString(), ageAsString -> {
            try {
                model.getAge().set(Integer.parseInt(ageAsString));
            } catch (Exception e) {
                log.error("A failure ocurred while setting the age string value from the age pin.");
            }
        });

        MonadicBinding<String> binding = EasyBind.combine(model.getAmount(),
                model.getAge(), this::calculateSimScore);
        scorePin = EasyBind.subscribe(binding, score -> model.getScore().set(score));
    }

    @Override
    public void onDeactivate() {
        agePin.unsubscribe();
        ageAsStringPin.unsubscribe();
        scorePin.unsubscribe();
    }

    private String calculateSimScore(String amount, Number age) {
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
        Navigation.navigateTo(NavigationTarget.BURN_BSQ_TAB_1);
    }

    void onNext() {
        Navigation.navigateTo(NavigationTarget.BURN_BSQ_TAB_3);
    }

    void onLearnMore() {
        Browser.open("https://bisq.wiki/reputation/burnBsq");
    }
}
