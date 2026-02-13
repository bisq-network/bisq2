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

import bisq.account.accounts.fiat.DomesticWireTransferAccountPayload;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;

public class DomesticWireTransferFormController extends FormController<DomesticWireTransferFormView, DomesticWireTransferFormModel, DomesticWireTransferAccountPayload> {
    public DomesticWireTransferFormController(ServiceProvider serviceProvider) {
        super(serviceProvider);
    }

    @Override
    protected DomesticWireTransferFormView createView() {
        return new DomesticWireTransferFormView(model, this);
    }

    @Override
    protected DomesticWireTransferFormModel createModel() {
        return new DomesticWireTransferFormModel(StringUtils.createUid());
    }

    @Override
    public void onActivate() {
        super.onActivate();
        model.getRunValidation().set(false);
    }

    @Override
    public boolean validate() {
        boolean holderNameValid = model.getHolderNameValidator().validateAndGet();
        boolean holderAddressValid = model.getHolderAddressValidator().validateAndGet();
        boolean bankNameValid = model.getBankNameValidator().validateAndGet();
        boolean bankIdValid = model.getBankIdValidator().validateAndGet();
        boolean accountNrValid = model.getAccountNrValidator().validateAndGet();
        model.getRunValidation().set(true);
        return holderNameValid && holderAddressValid && bankNameValid && bankIdValid && accountNrValid;
    }

    @Override
    public DomesticWireTransferAccountPayload createAccountPayload() {
        return new DomesticWireTransferAccountPayload(model.getId(),
                model.getHolderName().get(),
                model.getHolderAddress().get(),
                model.getBankName().get(),
                model.getBankId().get(),
                model.getAccountNr().get());
    }

    void onValidationDone() {
        model.getRunValidation().set(false);
    }
}
