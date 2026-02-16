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

import bisq.account.accounts.fiat.StrikeAccountPayload;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;

public class StrikeFormController extends FormController<StrikeFormView, StrikeFormModel, StrikeAccountPayload> {
    public StrikeFormController(ServiceProvider serviceProvider) {
        super(serviceProvider);
    }

    @Override
    protected StrikeFormView createView() {
        return new StrikeFormView(model, this);
    }

    @Override
    protected StrikeFormModel createModel() {
        return new StrikeFormModel(StringUtils.createUid());
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
        model.getRunValidation().set(true);
        return holderNameValid;
    }

    @Override
    public StrikeAccountPayload createAccountPayload() {
        return new StrikeAccountPayload(model.getId(), "US", model.getHolderName().get());
    }

    void onValidationDone() {
        model.getRunValidation().set(false);
    }
}
