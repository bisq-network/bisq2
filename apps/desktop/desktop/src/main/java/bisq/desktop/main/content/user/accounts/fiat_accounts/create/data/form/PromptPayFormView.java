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
public class PromptPayFormView extends FormView<PromptPayFormModel, PromptPayFormController> {
    private final MaterialTextField promptPayId;
    private Subscription runValidationPin;

    public PromptPayFormView(PromptPayFormModel model, PromptPayFormController controller) {
        super(model, controller);

        promptPayId = new MaterialTextField(Res.get("paymentAccounts.promptPay.promptPayId"),
                Res.get("paymentAccounts.createAccount.prompt", StringUtils.unCapitalize(Res.get("paymentAccounts.promptPay.promptPayId"))));
        promptPayId.setValidators(model.getPromptPayIdValidator());
        promptPayId.setMaxWidth(Double.MAX_VALUE);

        content.getChildren().add(promptPayId);
    }

    @Override
    protected void onViewAttached() {
        super.onViewAttached();
        if (StringUtils.isNotEmpty(model.getPromptPayId().get())) {
            promptPayId.setText(model.getPromptPayId().get());
            promptPayId.validate();
        }

        promptPayId.textProperty().bindBidirectional(model.getPromptPayId());

        runValidationPin = EasyBind.subscribe(model.getRunValidation(), runValidation -> {
            if (runValidation) {
                promptPayId.validate();
                controller.onValidationDone();
            }
        });
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();
        promptPayId.resetValidation();
        promptPayId.textProperty().unbindBidirectional(model.getPromptPayId());
        runValidationPin.unsubscribe();
    }
}
