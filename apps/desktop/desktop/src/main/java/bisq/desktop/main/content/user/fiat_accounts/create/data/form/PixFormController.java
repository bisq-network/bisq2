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

import bisq.account.accounts.fiat.PixAccountPayload;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PixFormController extends FormController<PixFormView, PixFormModel, PixAccountPayload> {
    public PixFormController(ServiceProvider serviceProvider) {
        super(serviceProvider);
    }

    @Override
    protected PixFormView createView() {
        return new PixFormView(model, this);
    }

    @Override
    protected PixFormModel createModel() {
        return new PixFormModel(StringUtils.createUid());
    }

    @Override
    public void onActivate() {
        model.getRunValidation().set(false);
    }

    @Override
    public void onDeactivate() {
    }

    @Override
    public PixAccountPayload createAccountPayload() {
        return new PixAccountPayload(model.getId(),
                "BR",
                model.getHolderName().get(),
                model.getPixKey().get());
    }

    @Override
    public boolean validate() {
        boolean holderNameValid = model.getHolderNameValidator().validateAndGet();
        boolean pixKeyValidatorValid = model.getPixKeyValidator().validateAndGet();
        model.getRunValidation().set(true);
        return holderNameValid && pixKeyValidatorValid;
    }

    void onValidationDone() {
        model.getRunValidation().set(false);
    }
}