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

package bisq.desktop.main.top;

import bisq.common.monetary.Coin;
import bisq.common.observable.Observable;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.components.UserProfileSelection;
import bisq.wallets.core.WalletService;
import lombok.Getter;

import javax.annotation.Nullable;
import java.util.Optional;

public class TopPanelController implements Controller {
    @Getter
    private final TopPanelView view;
    private final TopPanelModel model;
    private final Optional<WalletService> walletService;
    @Nullable
    private Pin balancePin;

    public TopPanelController(ServiceProvider serviceProvider) {
        walletService = serviceProvider.getWalletService();

        model = new TopPanelModel(serviceProvider.getWalletService().isPresent());
        UserProfileSelection userProfileSelection = new UserProfileSelection(serviceProvider);
        MarketPriceComponent marketPriceComponent = new MarketPriceComponent(serviceProvider);
        view = new TopPanelView(model, this, userProfileSelection, marketPriceComponent.getRoot());

    }

    @Override
    public void onActivate() {
        // Create a mock version of the actual code that would run if wallet service was present
        Coin fixedBalance = Coin.parseBtc("0.00013243"); // 0.0001324354 BTC in satoshis
        Observable<Coin> mockBalance = new Observable<>();
        mockBalance.set(fixedBalance);

        // Bind to our mock observable
        balancePin = FxBindings.bind(model.getBalanceAsCoinProperty())
                .to(mockBalance);
    }

    @Override
    public void onDeactivate() {
        if (balancePin != null) {
            balancePin.unbind();
        }
    }
}
