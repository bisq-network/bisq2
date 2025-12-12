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

package bisq.desktop.main.content.user.fiat_accounts.create.data.form;

import bisq.account.accounts.fiat.ZelleAccountPayload;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ZelleFormController extends FormController<ZelleFormView, ZelleFormModel, ZelleAccountPayload> {
    public ZelleFormController(ServiceProvider serviceProvider) {
        super(serviceProvider);
    }

    @Override
    protected ZelleFormView createView() {
        return new ZelleFormView(model, this);
    }

    @Override
    protected ZelleFormModel createModel() {
        return new ZelleFormModel(StringUtils.createUid());
    }

    @Override
    public void onActivate() {
        model.getRunValidation().set(false);
    }

    @Override
    public void onDeactivate() {
    }

    @Override
    public ZelleAccountPayload createAccountPayload() {
        return new ZelleAccountPayload(model.getId(),
                model.getHolderName().get(),
                model.getEmailOrMobileNr().get());
    }

    @Override
    public boolean validate() {
        boolean holderNameValid = model.getHolderNameValidator().validateAndGet();
        boolean emailOrPhoneNumberValid = model.getEmailOrPhoneNumberValidator().validateAndGet();
        model.getRunValidation().set(true);
        return holderNameValid && emailOrPhoneNumberValid;
    }

    void onValidationDone() {
        model.getRunValidation().set(false);
    }
}