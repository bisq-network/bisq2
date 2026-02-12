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

import bisq.account.accounts.fiat.FasterPaymentsAccountPayload;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FasterPaymentsFormController extends FormController<FasterPaymentsFormView, FasterPaymentsFormModel, FasterPaymentsAccountPayload> {
    public FasterPaymentsFormController(ServiceProvider serviceProvider) {
        super(serviceProvider);
    }

    @Override
    protected FasterPaymentsFormView createView() {
        return new FasterPaymentsFormView(model, this);
    }

    @Override
    protected FasterPaymentsFormModel createModel() {
        return new FasterPaymentsFormModel(StringUtils.createUid());
    }

    @Override
    public void onActivate() {
        super.onActivate();
        model.getRunValidation().set(false);
    }

    @Override
    public void onDeactivate() {
        super.onDeactivate();
    }

    @Override
    public FasterPaymentsAccountPayload createAccountPayload() {
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
        model.getRunValidation().set(true);
        return holderNameValid && sortCodeValid && accountNrValid;
    }

    void onValidationDone() {
        model.getRunValidation().set(false);
    }
}