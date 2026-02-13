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
public class SbpFormView extends FormView<SbpFormModel, SbpFormController> {
    private final MaterialTextField holderName;
    private final MaterialTextField mobileNumber;
    private final MaterialTextField bankName;
    private Subscription runValidationPin;

    public SbpFormView(SbpFormModel model, SbpFormController controller) {
        super(model, controller);

        holderName = new MaterialTextField(Res.get("paymentAccounts.holderName"),
                Res.get("paymentAccounts.createAccount.prompt", StringUtils.unCapitalize(Res.get("paymentAccounts.holderName"))));
        holderName.setValidators(model.getHolderNameValidator());
        holderName.setMaxWidth(Double.MAX_VALUE);

        mobileNumber = new MaterialTextField(Res.get("paymentAccounts.mobileNr"),
                Res.get("paymentAccounts.createAccount.prompt", StringUtils.unCapitalize(Res.get("paymentAccounts.mobileNr"))));
        mobileNumber.setValidators(model.getMobileNumberValidator());
        mobileNumber.setMaxWidth(Double.MAX_VALUE);

        bankName = new MaterialTextField(Res.get("paymentAccounts.bank.bankName"),
                Res.get("paymentAccounts.createAccount.prompt", StringUtils.unCapitalize(Res.get("paymentAccounts.bank.bankName"))));
        bankName.setValidators(model.getBankNameValidator());
        bankName.setMaxWidth(Double.MAX_VALUE);

        content.getChildren().addAll(holderName, mobileNumber, bankName);
        configOverlay(Res.get("paymentAccounts.createAccount.accountData.backgroundOverlay.sbp"));
    }

    @Override
    protected void onViewAttached() {
        super.onViewAttached();
        if (StringUtils.isNotEmpty(model.getHolderName().get())) {
            holderName.setText(model.getHolderName().get());
            holderName.validate();
        }
        if (StringUtils.isNotEmpty(model.getMobileNumber().get())) {
            mobileNumber.setText(model.getMobileNumber().get());
            mobileNumber.validate();
        }
        if (StringUtils.isNotEmpty(model.getBankName().get())) {
            bankName.setText(model.getBankName().get());
            bankName.validate();
        }

        holderName.textProperty().bindBidirectional(model.getHolderName());
        mobileNumber.textProperty().bindBidirectional(model.getMobileNumber());
        bankName.textProperty().bindBidirectional(model.getBankName());

        runValidationPin = EasyBind.subscribe(model.getRunValidation(), runValidation -> {
            if (runValidation) {
                holderName.validate();
                mobileNumber.validate();
                bankName.validate();
                controller.onValidationDone();
            }
        });
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();
        holderName.resetValidation();
        mobileNumber.resetValidation();
        bankName.resetValidation();

        holderName.textProperty().unbindBidirectional(model.getHolderName());
        mobileNumber.textProperty().unbindBidirectional(model.getMobileNumber());
        bankName.textProperty().unbindBidirectional(model.getBankName());

        runValidationPin.unsubscribe();
    }
}
