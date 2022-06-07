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

package bisq.desktop.primary.onboardingOld.onboardProTrader;

import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.View;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class OnboardProTraderView extends View<VBox, OnboardProTraderModel, OnboardProTraderController> {

    public OnboardProTraderView(OnboardProTraderModel model, OnboardProTraderController controller) {
        super(new VBox(), model, controller);

        //todo
        root.getChildren().addAll(new Label("SetupLiquidityProviderView"));
    }

    @Override
    protected void onViewAttached() {
        //todo
        UIScheduler.run(() -> Navigation.navigateTo(NavigationTarget.DISCUSS)).after(1000);
    }

    @Override
    protected void onViewDetached() {
    }
}
