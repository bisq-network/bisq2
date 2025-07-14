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

package bisq.desktop.main.content.user.fiat_accounts.create.data.payment_form;

import bisq.common.util.StringUtils;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.i18n.Res;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class FasterPaymentsPaymentFormView extends PaymentFormView<FasterPaymentsPaymentFormModel, FasterPaymentsPaymentFormController> {
    private final MaterialTextField holderName, sortCode, accountNr;
    private Subscription runValidationPin;

    public FasterPaymentsPaymentFormView(FasterPaymentsPaymentFormModel model,
                                         FasterPaymentsPaymentFormController controller) {
        super(model, controller);

        holderName = new MaterialTextField(Res.get("paymentAccounts.holderName"),
                Res.get("paymentAccounts.createAccount.prompt", StringUtils.unCapitalize(Res.get("paymentAccounts.holderName"))));
        holderName.setValidators(model.getHolderNameValidator());
        holderName.setMaxWidth(Double.MAX_VALUE);

        sortCode = new MaterialTextField(Res.get("paymentAccounts.fasterPayments.sortCode"),
                Res.get("paymentAccounts.createAccount.prompt", StringUtils.unCapitalize(Res.get("paymentAccounts.fasterPayments.sortCode"))));
        sortCode.setValidators(model.getSortCodeValidator(), model.getSortCodeNumberValidator());
        sortCode.setMaxWidth(Double.MAX_VALUE);

        accountNr = new MaterialTextField(Res.get("paymentAccounts.accountNr"),
                Res.get("paymentAccounts.createAccount.prompt", StringUtils.unCapitalize(Res.get("paymentAccounts.accountNr"))));
        accountNr.setValidators(model.getAccountNrValidator(), model.getAccountNrNumberValidator());
        accountNr.setMaxWidth(Double.MAX_VALUE);

        root.getChildren().addAll(holderName, sortCode, accountNr, Spacer.height(100));
    }

    @Override
    protected void onViewAttached() {
        if (StringUtils.isNotEmpty(model.getHolderName().get())) {
            holderName.setText(model.getHolderName().get());
            holderName.validate();
        }
        if (StringUtils.isNotEmpty(model.getSortCode().get())) {
            sortCode.setText(model.getSortCode().get());
            sortCode.validate();
        }
        if (StringUtils.isNotEmpty(model.getAccountNr().get())) {
            accountNr.setText(model.getAccountNr().get());
            accountNr.validate();
        }

        holderName.textProperty().bindBidirectional(model.getHolderName());
        sortCode.textProperty().bindBidirectional(model.getSortCode());
        accountNr.textProperty().bindBidirectional(model.getAccountNr());

        runValidationPin = EasyBind.subscribe(model.getRunValidation(), runValidation -> {
            if (runValidation) {
                holderName.validate();
                sortCode.validate();
                accountNr.validate();
                controller.onValidationDone();
            }
        });
    }

    @Override
    protected void onViewDetached() {
        holderName.resetValidation();
        sortCode.resetValidation();
        accountNr.resetValidation();

        holderName.textProperty().unbindBidirectional(model.getHolderName());
        sortCode.textProperty().unbindBidirectional(model.getSortCode());
        accountNr.textProperty().unbindBidirectional(model.getAccountNr());

        runValidationPin.unsubscribe();
    }
}