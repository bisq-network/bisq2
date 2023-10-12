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

package bisq.desktop.main.content.bisq_easy.wallet_guide.receive;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Browser;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.overlay.OverlayController;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WalletGuideReceiveController implements Controller {
    @Getter
    private final WalletGuideReceiveView view;

    public WalletGuideReceiveController(ServiceProvider serviceProvider) {
        WalletGuideReceiveModel model = new WalletGuideReceiveModel();
        view = new WalletGuideReceiveView(model, this);
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    void onBack() {
        Navigation.navigateTo(NavigationTarget.WALLET_GUIDE_CREATE_WALLET);
    }

    void onOpenLink1() {
        Browser.open("https://www.youtube.com/watch?v=imMX7i4qpmg&list=PLxdf8G0kzsUUE7HHNTGTWBFxzt2oudiyS&index=3");
    }

    void onOpenLink2() {
        Browser.open("https://www.youtube.com/watch?v=NqY3wBhloH4");
    }

    void onClose() {
        OverlayController.hide();
    }
}
