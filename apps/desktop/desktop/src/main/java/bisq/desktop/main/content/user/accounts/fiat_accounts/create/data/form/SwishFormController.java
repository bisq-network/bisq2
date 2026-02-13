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

import bisq.account.accounts.fiat.SwishAccountPayload;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;

public class SwishFormController extends FormController<SwishFormView, SwishFormModel, SwishAccountPayload> {
    public SwishFormController(ServiceProvider serviceProvider) {
        super(serviceProvider);
    }

    @Override
    protected SwishFormView createView() {
        return new SwishFormView(model, this);
    }

    @Override
    protected SwishFormModel createModel() {
        return new SwishFormModel(StringUtils.createUid());
    }

    @Override
    public void onActivate() {
        super.onActivate();
        model.getRunValidation().set(false);
    }

    @Override
    public boolean validate() {
        boolean holderNameValid = model.getHolderNameValidator().validateAndGet();
        boolean mobileValid = model.getMobileNrValidator().validateAndGet();
        model.getRunValidation().set(true);
        return holderNameValid && mobileValid;
    }

    @Override
    public SwishAccountPayload createAccountPayload() {
        return new SwishAccountPayload(model.getId(), "SE", model.getHolderName().get(), model.getMobileNr().get());
    }

    void onValidationDone() {
        model.getRunValidation().set(false);
    }
}
