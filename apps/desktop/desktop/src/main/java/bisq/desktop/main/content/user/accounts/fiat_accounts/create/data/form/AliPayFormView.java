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
public class AliPayFormView extends FormView<AliPayFormModel, AliPayFormController> {
    private final MaterialTextField accountNr;
    private Subscription runValidationPin;

    public AliPayFormView(AliPayFormModel model, AliPayFormController controller) {
        super(model, controller);

        accountNr = new MaterialTextField(Res.get("paymentAccounts.accountNr"),
                Res.get("paymentAccounts.createAccount.prompt", StringUtils.unCapitalize(Res.get("paymentAccounts.accountNr"))));
        accountNr.setValidators(model.getAccountNrValidator());
        accountNr.setMaxWidth(Double.MAX_VALUE);

        content.getChildren().add(accountNr);
    }

    @Override
    protected void onViewAttached() {
        super.onViewAttached();
        if (StringUtils.isNotEmpty(model.getAccountNr().get())) {
            accountNr.setText(model.getAccountNr().get());
            accountNr.validate();
        }

        accountNr.textProperty().bindBidirectional(model.getAccountNr());

        runValidationPin = EasyBind.subscribe(model.getRunValidation(), runValidation -> {
            if (runValidation) {
                accountNr.validate();
                controller.onValidationDone();
            }
        });
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();
        accountNr.resetValidation();
        accountNr.textProperty().unbindBidirectional(model.getAccountNr());
        runValidationPin.unsubscribe();
    }
}
