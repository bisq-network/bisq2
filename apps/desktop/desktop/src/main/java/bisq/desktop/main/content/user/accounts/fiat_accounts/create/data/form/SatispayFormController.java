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

import bisq.account.accounts.fiat.SatispayAccountPayload;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;

public class SatispayFormController extends FormController<SatispayFormView, SatispayFormModel, SatispayAccountPayload> {
    public SatispayFormController(ServiceProvider serviceProvider) {
        super(serviceProvider);
    }

    @Override
    protected SatispayFormView createView() {
        return new SatispayFormView(model, this);
    }

    @Override
    protected SatispayFormModel createModel() {
        return new SatispayFormModel(StringUtils.createUid());
    }

    @Override
    public void onActivate() {
        super.onActivate();
        model.getRunValidation().set(false);

        showOverlay();
    }

    @Override
    public boolean validate() {
        boolean holderNameValid = model.getHolderNameValidator().validateAndGet();
        boolean mobileValid = model.getMobileNrValidator().validateAndGet();
        model.getRunValidation().set(true);
        return holderNameValid && mobileValid;
    }

    @Override
    public SatispayAccountPayload createAccountPayload() {
        return new SatispayAccountPayload(model.getId(), model.getHolderName().get(), model.getMobileNr().get());
    }

    void onValidationDone() {
        model.getRunValidation().set(false);
    }
}
