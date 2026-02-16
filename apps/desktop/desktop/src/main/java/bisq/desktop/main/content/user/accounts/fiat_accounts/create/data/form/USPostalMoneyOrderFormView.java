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

package bisq.desktop.main.content.user.accounts.fiat_accounts.create.data.form;

import bisq.common.util.StringUtils;
import bisq.desktop.components.controls.MaterialTextArea;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.i18n.Res;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class USPostalMoneyOrderFormView extends FormView<USPostalMoneyOrderFormModel, USPostalMoneyOrderFormController> {
    private final MaterialTextField holderName;
    private final MaterialTextArea postalAddress;
    private Subscription runValidationPin;

    public USPostalMoneyOrderFormView(USPostalMoneyOrderFormModel model, USPostalMoneyOrderFormController controller) {
        super(model, controller);

        holderName = new MaterialTextField(Res.get("paymentAccounts.holderName"),
                Res.get("paymentAccounts.createAccount.prompt", StringUtils.unCapitalize(Res.get("paymentAccounts.holderName"))));
        holderName.setValidators(model.getHolderNameValidator());
        holderName.setMaxWidth(Double.MAX_VALUE);

        postalAddress = new MaterialTextArea(Res.get("paymentAccounts.postalAddress"),
                Res.get("paymentAccounts.createAccount.prompt", StringUtils.unCapitalize(Res.get("paymentAccounts.postalAddress"))));
        postalAddress.setValidators(model.getPostalAddressValidator());
        postalAddress.setFixedHeight(120);

        content.getChildren().addAll(holderName, postalAddress);

        configOverlay(Res.get("paymentAccounts.createAccount.accountData.backgroundOverlay.usPostalMoneyOrder"));
    }

    @Override
    protected void onViewAttached() {
        super.onViewAttached();
        if (StringUtils.isNotEmpty(model.getHolderName().get())) {
            holderName.setText(model.getHolderName().get());
            holderName.validate();
        }
        if (StringUtils.isNotEmpty(model.getPostalAddress().get())) {
            postalAddress.setText(model.getPostalAddress().get());
            postalAddress.validate();
        }

        holderName.textProperty().bindBidirectional(model.getHolderName());
        postalAddress.textProperty().bindBidirectional(model.getPostalAddress());

        runValidationPin = EasyBind.subscribe(model.getRunValidation(), runValidation -> {
            if (runValidation) {
                holderName.validate();
                postalAddress.validate();
                controller.onValidationDone();
            }
        });
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();
        holderName.resetValidation();
        postalAddress.resetValidation();

        holderName.textProperty().unbindBidirectional(model.getHolderName());
        postalAddress.textProperty().unbindBidirectional(model.getPostalAddress());

        runValidationPin.unsubscribe();
    }
}
