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

import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import javafx.scene.layout.Region;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SetupWalletWizardProtectController implements Controller {
    @Getter
    private final SetupWalletWizardProtectModel model;
    @Getter
    private final SetupWalletWizardProtectView view;

    public SetupWalletWizardProtectController() {
        model = new SetupWalletWizardProtectModel();
        view = new SetupWalletWizardProtectView(model, this);
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    public boolean isValid() {
        String password = model.getPassword().get();
        String confirmPassword = model.getConfirmPassword().get();
        return model.getPasswordRulesValidator().validateAndGet() && password.equals(confirmPassword);
    }

    public void handleInvalidInput() {
        String password = model.getPassword().get();
        String confirmPassword = model.getConfirmPassword().get();

        if (model.getPasswordRulesValidator().validateAndGet() && !password.equals(confirmPassword)) {
            new Popup().invalid(Res.get("wallet.protectWallet.error.passwordsDontMatch"))
                    .owner(getPopupOwner())
                    .show();
        }
    }

    public String getPassword() {
        return model.getPassword().get();
    }

    public void setInputLocked(boolean locked) {
        model.getInputLocked().set(locked);
    }

    private Region getPopupOwner() {
        return (Region) view.getRoot().getParent().getParent();
    }
}
