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
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import javafx.scene.layout.Region;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CreateWalletProtectController implements Controller {

    private final CreateWalletProtectModel model;
    @Getter
    private final CreateWalletProtectView view;

    public CreateWalletProtectController(ServiceProvider serviceProvider) {
        model = new CreateWalletProtectModel();
        view = new CreateWalletProtectView(model, this);
    }

    public boolean isValid() {
        String password = model.getPassword().get();
        String confirmPassword = model.getConfirmPassword().get();

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
    }

    public CreateWalletProtectModel getModel() {
        return model;
    }

}
