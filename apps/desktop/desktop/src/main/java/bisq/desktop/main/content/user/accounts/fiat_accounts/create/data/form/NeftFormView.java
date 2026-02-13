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
public class NeftFormView extends FormView<NeftFormModel, NeftFormController> {
    private final MaterialTextField holderName;
    private final MaterialTextField accountNr;
    private final MaterialTextField ifsc;
    private Subscription runValidationPin;

    public NeftFormView(NeftFormModel model, NeftFormController controller) {
        super(model, controller);

        holderName = new MaterialTextField(Res.get("paymentAccounts.holderName"),
                Res.get("paymentAccounts.createAccount.prompt", StringUtils.unCapitalize(Res.get("paymentAccounts.holderName"))));
        holderName.setValidators(model.getHolderNameValidator());
        holderName.setMaxWidth(Double.MAX_VALUE);

        accountNr = new MaterialTextField(Res.get("paymentAccounts.accountNr"),
                Res.get("paymentAccounts.createAccount.prompt", StringUtils.unCapitalize(Res.get("paymentAccounts.accountNr"))));
        accountNr.setValidators(model.getAccountNrValidator());
        accountNr.setMaxWidth(Double.MAX_VALUE);

        ifsc = new MaterialTextField(Res.get("paymentAccounts.createAccount.accountData.ifsc"),
                Res.get("paymentAccounts.createAccount.prompt", StringUtils.unCapitalize(Res.get("paymentAccounts.createAccount.accountData.ifsc"))));
        ifsc.setValidators(model.getIfscValidator());
        ifsc.setMaxWidth(Double.MAX_VALUE);

        content.getChildren().addAll(holderName, accountNr, ifsc);

        configOverlay(Res.get("paymentAccounts.createAccount.accountData.backgroundOverlay.neft"));
    }

    @Override
    protected void onViewAttached() {
        super.onViewAttached();
        if (StringUtils.isNotEmpty(model.getHolderName().get())) {
            holderName.setText(model.getHolderName().get());
            holderName.validate();
        }
        if (StringUtils.isNotEmpty(model.getAccountNr().get())) {
            accountNr.setText(model.getAccountNr().get());
            accountNr.validate();
        }
        if (StringUtils.isNotEmpty(model.getIfsc().get())) {
            ifsc.setText(model.getIfsc().get());
            ifsc.validate();
        }

        holderName.textProperty().bindBidirectional(model.getHolderName());
        accountNr.textProperty().bindBidirectional(model.getAccountNr());
        ifsc.textProperty().bindBidirectional(model.getIfsc());

        runValidationPin = EasyBind.subscribe(model.getRunValidation(), runValidation -> {
            if (runValidation) {
                holderName.validate();
                accountNr.validate();
                ifsc.validate();
                controller.onValidationDone();
            }
        });
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();
        holderName.resetValidation();
        accountNr.resetValidation();
        ifsc.resetValidation();

        holderName.textProperty().unbindBidirectional(model.getHolderName());
        accountNr.textProperty().unbindBidirectional(model.getAccountNr());
        ifsc.textProperty().unbindBidirectional(model.getIfsc());

        runValidationPin.unsubscribe();
    }
}
