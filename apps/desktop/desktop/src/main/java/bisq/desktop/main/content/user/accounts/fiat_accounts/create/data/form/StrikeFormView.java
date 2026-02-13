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
public class StrikeFormView extends FormView<StrikeFormModel, StrikeFormController> {
    private final MaterialTextField holderName;
    private Subscription runValidationPin;

    public StrikeFormView(StrikeFormModel model, StrikeFormController controller) {
        super(model, controller);

        holderName = new MaterialTextField(Res.get("paymentAccounts.userName"),
                Res.get("paymentAccounts.createAccount.prompt", StringUtils.unCapitalize(Res.get("paymentAccounts.userName"))));
        holderName.setValidators(model.getHolderNameValidator());
        holderName.setMaxWidth(Double.MAX_VALUE);

        content.getChildren().add(holderName);

        configOverlay(Res.get("paymentAccounts.createAccount.accountData.backgroundOverlay.strike"));
    }

    @Override
    protected void onViewAttached() {
        super.onViewAttached();
        if (StringUtils.isNotEmpty(model.getHolderName().get())) {
            holderName.setText(model.getHolderName().get());
            holderName.validate();
        }

        holderName.textProperty().bindBidirectional(model.getHolderName());

        runValidationPin = EasyBind.subscribe(model.getRunValidation(), runValidation -> {
            if (runValidation) {
                holderName.validate();
                controller.onValidationDone();
            }
        });
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();
        holderName.resetValidation();
        holderName.textProperty().unbindBidirectional(model.getHolderName());
        runValidationPin.unsubscribe();
    }
}
