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

package bisq.desktop.main.content.user.accounts.create.data.payment_form.aaa;

import bisq.account.accounts.ZelleAccountPayload;
import bisq.desktop.ServiceProvider;
import bisq.desktop.main.content.user.accounts.create.data.payment_form.PaymentFormController;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
public class ZellePaymentFormController extends PaymentFormController<ZellePaymentFormView, ZellePaymentFormModel, ZelleAccountPayload> {
    public ZellePaymentFormController(ServiceProvider serviceProvider) {
        super(serviceProvider);
    }

    @Override
    protected ZellePaymentFormView createView() {
        return new ZellePaymentFormView(model, this);
    }

    @Override
    protected ZellePaymentFormModel createModel() {
        return new ZellePaymentFormModel(UUID.randomUUID().toString());
    }

    @Override
    public void onActivate() {
        model.getRequireValidation().set(false);
    }

    @Override
    public void onDeactivate() {
    }

    @Override
    public ZelleAccountPayload getAccountPayload() {
        return new ZelleAccountPayload(model.getId(),
                model.getHolderName().get(),
                model.getEmailOrMobileNr().get());
    }

    @Override
    public boolean validate() {
        boolean holderNameValid = model.getHolderNameValidator().validateAndGet();
        boolean emailOrPhoneNumberValid = model.getEmailOrPhoneNumberValidator().validateAndGet();
        model.getRequireValidation().set(true);
        return holderNameValid && emailOrPhoneNumberValid;
    }

    void onValidationDone() {
        model.getRequireValidation().set(false);
    }
}