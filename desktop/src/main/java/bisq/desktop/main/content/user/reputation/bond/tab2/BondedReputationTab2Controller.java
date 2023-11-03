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

package bisq.desktop.main.content.user.reputation.bond.tab2;

import bisq.common.util.MathUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Browser;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.user.reputation.BondedReputationService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;
import org.fxmisc.easybind.monadic.MonadicBinding;

@Slf4j
public class BondedReputationTab2Controller implements Controller {
    @Getter
    private final BondedReputationTab2View view;
    private Subscription agePin, ageAsStringPin, scorePin;

    private final BondedReputationTab2Model model;

    public BondedReputationTab2Controller(ServiceProvider serviceProvider) {
        model = new BondedReputationTab2Model();
        view = new BondedReputationTab2View(model, this);

        model.getAmount().set("100");
        model.getLockTime().set("10000");
        model.getAgeAsInt().set(0);
        model.getAge().set("0");
    }

    @Override
    public void onActivate() {
        agePin = EasyBind.subscribe(model.getAgeAsInt(), age -> model.getAge().set(String.valueOf(age)));
        ageAsStringPin = EasyBind.subscribe(model.getAge(), ageAsString -> {
            try {
                model.getAgeAsInt().set(Integer.parseInt(ageAsString));
            } catch (Exception e) {
                log.error("A failure ocurred while setting the age string value from the age pin.");
            }
        });

        MonadicBinding<String> binding = EasyBind.combine(model.getAmount(), model.getLockTime(), model.getAgeAsInt(), this::calculateSimScore);
        scorePin = EasyBind.subscribe(binding, score -> model.getScore().set(score));
    }

    @Override
    public void onDeactivate() {
        agePin.unsubscribe();
        ageAsStringPin.unsubscribe();
        scorePin.unsubscribe();
    }

    private String calculateSimScore(String amount, String lockTime, Number age) {
        try {
            // amountAsLong is the smallest unit of BSQ (100 = 1 BSQ)
            long amountAsLong = MathUtils.roundDoubleToLong(Double.parseDouble(amount) * 100);
            long lockTimeAsLong = Long.parseLong(lockTime);
            long ageInDays = age.intValue();
            long totalScore = BondedReputationService.doCalculateScore(amountAsLong, lockTimeAsLong, ageInDays);
            return String.valueOf(totalScore);
        } catch (Exception e) {
            return "";
        }
    }

    void onBack() {
        Navigation.navigateTo(NavigationTarget.BSQ_BOND_TAB_1);
    }

    void onNext() {
        Navigation.navigateTo(NavigationTarget.BSQ_BOND_TAB_3);
    }

    void onLearnMore() {
        Browser.open("https://bisq.wiki/reputation/bondedBsq");
    }
}
