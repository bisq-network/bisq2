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

package bisq.desktop.main.content.wallet.create_wallet.protect;

import bisq.bisq_easy.BisqEasyTradeAmountLimits;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.currency.Market;
import bisq.common.monetary.Monetary;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Browser;
import bisq.desktop.common.utils.KeyHandlerUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.bisq_easy.components.amount_selection.AmountSelectionController;
import bisq.i18n.Res;
import bisq.offer.Direction;
import bisq.offer.amount.OfferAmountUtil;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.PriceUtil;
import bisq.presentation.formatters.AmountFormatter;
import bisq.presentation.formatters.PriceFormatter;
import bisq.user.identity.UserIdentityService;
import bisq.user.reputation.ReputationService;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.scene.input.KeyEvent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
public class CreateWalletProtectController implements Controller {
    private final CreateWalletProtectModel model;
    @Getter
    private final CreateWalletProtectView view;
    private final Consumer<Boolean> navigationButtonsVisibleHandler;

    public CreateWalletProtectController(ServiceProvider serviceProvider,
                                         Consumer<Boolean> navigationButtonsVisibleHandler) {
        this.navigationButtonsVisibleHandler = navigationButtonsVisibleHandler;
        model = new CreateWalletProtectModel();
        view = new CreateWalletProtectView(model, this);
    }

    public void init(BisqEasyOffer bisqEasyOffer) {

    }

    @Override
    public void onActivate() {

    }

    @Override
    public void onDeactivate() {
        navigationButtonsVisibleHandler.accept(true);
    }

    void onKeyPressedWhileShowingOverlay(KeyEvent keyEvent) {
        KeyHandlerUtil.handleEnterKeyEvent(keyEvent, () -> {
        });
    }

}
