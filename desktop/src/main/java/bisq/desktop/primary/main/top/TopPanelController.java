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

package bisq.desktop.primary.main.top;

import bisq.application.DefaultApplicationService;
import bisq.common.observable.Pin;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.view.Controller;
import bisq.desktop.primary.main.content.components.UserProfileSelection;
import bisq.wallets.electrum.ElectrumWalletService;
import lombok.Getter;

public class TopPanelController implements Controller {
    private final ElectrumWalletService electrumWalletService;
    @Getter
    private final TopPanelView view;
    private final TopPanelModel model;
    private Pin balancePin;

    public TopPanelController(DefaultApplicationService applicationService) {
        electrumWalletService = applicationService.getElectrumWalletService();

        model = new TopPanelModel();
        UserProfileSelection userProfileSelection = new UserProfileSelection(applicationService.getUserService().getUserIdentityService());
        MarketSelection marketSelection = new MarketSelection(applicationService.getOracleService().getMarketPriceService());
        view = new TopPanelView(model, this, userProfileSelection, marketSelection.getRoot());

    }

    @Override
    public void onActivate() {
        balancePin = FxBindings.bind(model.getBalanceAsCoinProperty())
                .to(electrumWalletService.getBalance());
    }

    @Override
    public void onDeactivate() {
        balancePin.unbind();
    }
}
