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

import bisq.account.accounts.fiat.Pin4AccountPayload;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;

public class Pin4FormController extends FormController<Pin4FormView, Pin4FormModel, Pin4AccountPayload> {
    public Pin4FormController(ServiceProvider serviceProvider) {
        super(serviceProvider);
    }

    @Override
    protected Pin4FormView createView() {
        return new Pin4FormView(model, this);
    }

    @Override
    protected Pin4FormModel createModel() {
        return new Pin4FormModel(StringUtils.createUid());
    }

    @Override
    public void onActivate() {
        super.onActivate();
        model.getRunValidation().set(false);
    }

    @Override
    public boolean validate() {
        boolean mobileValid = model.getMobileNrValidator().validateAndGet();
        model.getRunValidation().set(true);
        return mobileValid;
    }

    @Override
    public Pin4AccountPayload createAccountPayload() {
        return new Pin4AccountPayload(model.getId(), model.getMobileNr().get());
    }

    void onValidationDone() {
        model.getRunValidation().set(false);
    }
}
