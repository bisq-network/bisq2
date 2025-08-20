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

package bisq.desktop.main.content.wallet.setup_wallet_wizard.protect;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import javafx.scene.layout.Region;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

@Slf4j
public class SetupWalletWizardProtectController implements Controller {
    @Getter
    private final SetupWalletWizardProtectModel model;
    @Getter
    private final SetupWalletWizardProtectView view;
    private final Runnable onNextHandler;
    private final Consumer<Boolean> navigationButtonsVisibleHandler;

    public SetupWalletWizardProtectController(ServiceProvider serviceProvider,
                                              Runnable onNextHandler,
                                              Consumer<Boolean> navigationButtonsVisibleHandler) {
        model = new SetupWalletWizardProtectModel();
        view = new SetupWalletWizardProtectView(model, this);
        this.onNextHandler = onNextHandler;
        this.navigationButtonsVisibleHandler = navigationButtonsVisibleHandler;
    }

    @Override
    public void onActivate() {
        model.setSkipProtectStep(false);
    }

    @Override
    public void onDeactivate() {
    }

    public void handleSkipProtectStep() {
        model.getShouldShowSkipProtectStepOverlay().set(true);
        navigationButtonsVisibleHandler.accept(false);
    }

    public boolean isValid() {
        String password = model.getPassword().get();
        String confirmPassword = model.getConfirmPassword().get();
        boolean passwordValid = model.getPasswordValidator().validateAndGet();
        boolean confirmPasswordValid = model.getConfirmPasswordValidator().validateAndGet();
        boolean isPasswordSetupCorrectly = passwordValid && confirmPasswordValid && password.equals(confirmPassword);
        boolean shouldSkipThisStep = model.isSkipProtectStep();
        return isPasswordSetupCorrectly || shouldSkipThisStep;
    }

    public void handleInvalidInput() {
        String password = model.getPassword().get();
        String confirmPassword = model.getConfirmPassword().get();

        if (model.getPasswordValidator().validateAndGet() && model.getConfirmPasswordValidator().validateAndGet()) {
            if (!password.equals(confirmPassword)) {
                new Popup().invalid(Res.get("wallet.protectWallet.error.passwordsDontMatch"))
                        .owner(getPopupOwner())
                        .show();
            }
        }
    }

    public String getPassword() {
        return model.getPassword().get();
    }

    void onDoSkipProtectStep() {
        model.getShouldShowSkipProtectStepOverlay().set(false);
        navigationButtonsVisibleHandler.accept(true);
        model.setSkipProtectStep(true);
        onNextHandler.run();
    }

    void onCloseOverlay() {
        model.getShouldShowSkipProtectStepOverlay().set(false);
        navigationButtonsVisibleHandler.accept(true);
    }

    private Region getPopupOwner() {
        return (Region) view.getRoot().getParent().getParent();
    }
}
