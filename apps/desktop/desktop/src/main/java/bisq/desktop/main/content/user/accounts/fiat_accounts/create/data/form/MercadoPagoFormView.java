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
import bisq.desktop.components.controls.MaterialTextField;
import bisq.i18n.Res;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class MercadoPagoFormView extends FormView<MercadoPagoFormModel, MercadoPagoFormController> {
    private final MaterialTextField holderName;
    private final MaterialTextField holderId;
    private Subscription runValidationPin;

    public MercadoPagoFormView(MercadoPagoFormModel model, MercadoPagoFormController controller) {
        super(model, controller);

        holderName = new MaterialTextField(Res.get("paymentAccounts.holderName"),
                Res.get("paymentAccounts.createAccount.prompt", StringUtils.unCapitalize(Res.get("paymentAccounts.holderName"))));
        holderName.setValidators(model.getHolderNameValidator());
        holderName.setMaxWidth(Double.MAX_VALUE);

        holderId = new MaterialTextField(Res.get("paymentAccounts.mercadoPago.holderId"),
                Res.get("paymentAccounts.mercadoPago.holderId.prompt"));
        holderId.setValidators(model.getHolderIdValidator());
        holderId.setMaxWidth(Double.MAX_VALUE);


        content.getChildren().addAll(holderName, holderId);
    }

    @Override
    protected void onViewAttached() {
        super.onViewAttached();
        if (StringUtils.isNotEmpty(model.getHolderId().get())) {
            holderId.setText(model.getHolderId().get());
            holderId.validate();
        }
        if (StringUtils.isNotEmpty(model.getHolderName().get())) {
            holderName.setText(model.getHolderName().get());
            holderName.validate();
        }

        holderId.textProperty().bindBidirectional(model.getHolderId());
        holderName.textProperty().bindBidirectional(model.getHolderName());

        runValidationPin = EasyBind.subscribe(model.getRunValidation(), runValidation -> {
            if (runValidation) {
                holderId.validate();
                holderName.validate();
                controller.onValidationDone();
            }
        });
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();
        holderId.resetValidation();
        holderName.resetValidation();

        holderId.textProperty().unbindBidirectional(model.getHolderId());
        holderName.textProperty().unbindBidirectional(model.getHolderName());

        runValidationPin.unsubscribe();
    }
}
