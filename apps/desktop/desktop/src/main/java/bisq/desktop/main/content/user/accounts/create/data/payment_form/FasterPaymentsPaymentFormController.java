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

import bisq.account.accounts.FasterPaymentsAccountPayload;
import bisq.desktop.ServiceProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
public class FasterPaymentsPaymentFormController extends PaymentFormController<FasterPaymentsPaymentFormView, FasterPaymentsPaymentFormModel, FasterPaymentsAccountPayload> {
    public FasterPaymentsPaymentFormController(ServiceProvider serviceProvider) {
        super(serviceProvider);
    }

    @Override
    protected FasterPaymentsPaymentFormView createView() {
        return new FasterPaymentsPaymentFormView(model, this);
    }

    @Override
    protected FasterPaymentsPaymentFormModel createModel() {
        return new FasterPaymentsPaymentFormModel(UUID.randomUUID().toString());
    }

    @Override
    public void onActivate() {
        model.getRequireValidation().set(false);
    }

    @Override
    public void onDeactivate() {
    }

    @Override
    public FasterPaymentsAccountPayload getAccountPayload() {
        return new FasterPaymentsAccountPayload(model.getId(),
                model.getHolderName().get(),
                model.getSortCode().get(),
                model.getAccountNr().get());
    }

    @Override
    public boolean validate() {
        boolean holderNameValid = model.getHolderNameValidator().validateAndGet();
        boolean sortCodeValid = model.getSortCodeValidator().validateAndGet() && model.getSortCodeNumberValidator().validateAndGet();
        boolean accountNrValid = model.getAccountNrValidator().validateAndGet() && model.getAccountNrNumberValidator().validateAndGet();
        model.getRequireValidation().set(true);
        return holderNameValid && sortCodeValid && accountNrValid;
    }

    void onValidationDone() {
        model.getRequireValidation().set(false);
    }
}