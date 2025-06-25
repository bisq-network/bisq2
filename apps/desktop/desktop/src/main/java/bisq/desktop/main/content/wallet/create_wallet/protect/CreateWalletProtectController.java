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

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.utils.KeyHandlerUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.wallet.create_wallet.CreateWalletModel;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.wallets.core.WalletService;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

@Slf4j
public class CreateWalletProtectController implements Controller {
    private static final Logger log = LoggerFactory.getLogger(CreateWalletProtectController.class);
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

    public boolean isValid() {
        String password = model.getPassword().get();
        String confirmPassword = model.getConfirmPassword().get();

        log.error("model.getPassword().get(): ");
        log.error(password);
        log.error(confirmPassword);
        return !password.isEmpty() && password.equals(confirmPassword);
    }

    public void handleInvalidInput() {
        String password = model.getPassword().get();
        String confirmPassword = model.getConfirmPassword().get();

        if (password.isEmpty()) {
            new Popup().invalid("Please enter password")
                    .owner((Region) view.getRoot().getParent().getParent())
                    .show();
        } else if (confirmPassword.isEmpty()) {
            new Popup().invalid("Please enter password again")
                    .owner((Region) view.getRoot().getParent().getParent())
                    .show();
        } else if (!password.equals(confirmPassword)) {
            new Popup().invalid("Passwords don't match")
                    .owner((Region) view.getRoot().getParent().getParent())
                    .show();
        }
    }

    @Override
    public void onActivate() {

    }

    @Override
    public void onDeactivate() {
        navigationButtonsVisibleHandler.accept(true);
    }

    public CreateWalletProtectModel getModel() {
        return model;
    }

    void onKeyPressedWhileShowingOverlay(KeyEvent keyEvent) {
        KeyHandlerUtil.handleEnterKeyEvent(keyEvent, () -> {
        });
    }

}
