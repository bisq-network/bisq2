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

package bisq.desktop.main.content.user.fiat_accounts.create.data.form;

import bisq.common.util.StringUtils;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.i18n.Res;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class PixFormView extends FormView<PixFormModel, PixFormController> {
    private final MaterialTextField holderName, pixKey;
    private Subscription runValidationPin;

    public PixFormView(PixFormModel model, PixFormController controller) {
        super(model, controller);

        holderName = new MaterialTextField(Res.get("paymentAccounts.holderName"),
                Res.get("paymentAccounts.createAccount.prompt", StringUtils.unCapitalize(Res.get("paymentAccounts.holderName"))));
        holderName.setValidators(model.getHolderNameValidator());
        holderName.setMaxWidth(Double.MAX_VALUE);

        pixKey = new MaterialTextField(Res.get("paymentAccounts.pix.pixKey"),
                Res.get("paymentAccounts.createAccount.prompt", Res.get("paymentAccounts.pix.pixKey")));
        pixKey.setValidators(model.getPixKeyValidator());
        pixKey.setMaxWidth(Double.MAX_VALUE);

        root.getChildren().addAll(holderName, pixKey, Spacer.height(100));
    }

    @Override
    protected void onViewAttached() {
        if (StringUtils.isNotEmpty(model.getHolderName().get())) {
            holderName.setText(model.getHolderName().get());
            holderName.validate();
        }
        if (StringUtils.isNotEmpty(model.getPixKey().get())) {
            pixKey.setText(model.getPixKey().get());
            pixKey.validate();
        }

        holderName.textProperty().bindBidirectional(model.getHolderName());
        pixKey.textProperty().bindBidirectional(model.getPixKey());

        runValidationPin = EasyBind.subscribe(model.getRunValidation(), runValidation -> {
            if (runValidation) {
                holderName.validate();
                pixKey.validate();
                controller.onValidationDone();
            }
        });
    }

    @Override
    protected void onViewDetached() {
        holderName.resetValidation();
        pixKey.resetValidation();

        holderName.textProperty().unbindBidirectional(model.getHolderName());
        pixKey.textProperty().unbindBidirectional(model.getPixKey());

        runValidationPin.unsubscribe();
    }
}