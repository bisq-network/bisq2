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

package bisq.desktop.main.content.user.accounts.create.data.payment_form;

import bisq.account.accounts.PixAccountPayload;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PixPaymentFormController extends PaymentFormController<PixPaymentFormView, PixPaymentFormModel, PixAccountPayload> {
    public PixPaymentFormController(ServiceProvider serviceProvider) {
        super(serviceProvider);
    }

    @Override
    protected PixPaymentFormView createView() {
        return new PixPaymentFormView(model, this);
    }

    @Override
    protected PixPaymentFormModel createModel() {
        return new PixPaymentFormModel(StringUtils.createUid());
    }

    @Override
    public void onActivate() {
        model.getRequireValidation().set(false);
    }

    @Override
    public void onDeactivate() {
    }

    @Override
    public PixAccountPayload getAccountPayload() {
        return new PixAccountPayload(model.getId(),
                "BR",
                model.getHolderName().get(),
                model.getPixKey().get());
    }

    @Override
    public boolean validate() {
        boolean holderNameValid = model.getHolderNameValidator().validateAndGet();
        boolean pixKeyValidatorValid = model.getPixKeyValidator().validateAndGet();
        model.getRequireValidation().set(true);
        return holderNameValid && pixKeyValidatorValid;
    }

    void onValidationDone() {
        model.getRequireValidation().set(false);
    }
}