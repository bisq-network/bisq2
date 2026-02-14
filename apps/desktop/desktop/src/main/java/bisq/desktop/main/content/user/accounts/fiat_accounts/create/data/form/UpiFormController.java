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

import bisq.account.accounts.fiat.UpiAccountPayload;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;

public class UpiFormController extends FormController<UpiFormView, UpiFormModel, UpiAccountPayload> {
    public UpiFormController(ServiceProvider serviceProvider) {
        super(serviceProvider);
    }

    @Override
    protected UpiFormView createView() {
        return new UpiFormView(model, this);
    }

    @Override
    protected UpiFormModel createModel() {
        return new UpiFormModel(StringUtils.createUid());
    }

    @Override
    public void onActivate() {
        super.onActivate();
        model.getRunValidation().set(false);
        showOverlay();
    }

    @Override
    public boolean validate() {
        boolean addressValid = model.getVirtualPaymentAddressValidator().validateAndGet();
        model.getRunValidation().set(true);
        return addressValid;
    }

    @Override
    public UpiAccountPayload createAccountPayload() {
        return new UpiAccountPayload(model.getId(), "IN", model.getVirtualPaymentAddress().get());
    }

    void onValidationDone() {
        model.getRunValidation().set(false);
    }
}
