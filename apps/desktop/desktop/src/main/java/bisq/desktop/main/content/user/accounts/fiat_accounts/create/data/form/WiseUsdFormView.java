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
public class WiseUsdFormView extends FormView<WiseUsdFormModel, WiseUsdFormController> {
    private final MaterialTextField holderName, email;
    private final MaterialTextArea beneficiaryAddress;
    private Subscription runValidationPin;

    public WiseUsdFormView(WiseUsdFormModel model, WiseUsdFormController controller) {
        super(model, controller);

        holderName = new MaterialTextField(Res.get("paymentAccounts.holderName"),
                Res.get("paymentAccounts.createAccount.prompt", StringUtils.unCapitalize(Res.get("paymentAccounts.holderName"))));
        holderName.setValidators(model.getHolderNameValidator());
        holderName.setMaxWidth(Double.MAX_VALUE);

        email = new MaterialTextField(Res.get("paymentAccounts.email"),
                Res.get("paymentAccounts.createAccount.prompt", StringUtils.unCapitalize(Res.get("paymentAccounts.email"))));
        email.setValidators(model.getEmailValidator());
        email.setMaxWidth(Double.MAX_VALUE);

        beneficiaryAddress = new MaterialTextArea(Res.get("paymentAccounts.wiseUsd.beneficiaryAddress"),
                Res.get("paymentAccounts.createAccount.prompt", StringUtils.unCapitalize(Res.get("paymentAccounts.wiseUsd.beneficiaryAddress"))));
        beneficiaryAddress.setValidators(model.getBeneficiaryAddressValidator());
        beneficiaryAddress.setFixedHeight(120);

        content.getChildren().addAll(holderName, email, beneficiaryAddress);
    }

    @Override
    protected void onViewAttached() {
        super.onViewAttached();
        if (StringUtils.isNotEmpty(model.getHolderName().get())) {
            holderName.setText(model.getHolderName().get());
            holderName.validate();
        }
        if (StringUtils.isNotEmpty(model.getEmail().get())) {
            email.setText(model.getEmail().get());
            email.validate();
        }
        if (StringUtils.isNotEmpty(model.getBeneficiaryAddress().get())) {
            beneficiaryAddress.setText(model.getBeneficiaryAddress().get());
            beneficiaryAddress.validate();
        }

        holderName.textProperty().bindBidirectional(model.getHolderName());
        email.textProperty().bindBidirectional(model.getEmail());
        beneficiaryAddress.textProperty().bindBidirectional(model.getBeneficiaryAddress());

        runValidationPin = EasyBind.subscribe(model.getRunValidation(), runValidation -> {
            if (runValidation) {
                holderName.validate();
                email.validate();
                beneficiaryAddress.validate();
                controller.onValidationDone();
            }
        });
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();
        holderName.resetValidation();
        email.resetValidation();
        beneficiaryAddress.resetValidation();

        holderName.textProperty().unbindBidirectional(model.getHolderName());
        email.textProperty().unbindBidirectional(model.getEmail());
        beneficiaryAddress.textProperty().unbindBidirectional(model.getBeneficiaryAddress());

        runValidationPin.unsubscribe();
    }
}
