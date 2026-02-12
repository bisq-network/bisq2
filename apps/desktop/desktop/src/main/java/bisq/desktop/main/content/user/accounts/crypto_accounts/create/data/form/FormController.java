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

package bisq.desktop.main.content.user.accounts.crypto_accounts.create.data.form;

import bisq.account.accounts.crypto.CryptoAssetAccountPayload;
import bisq.account.payment_method.DigitalAssetPaymentMethod;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import lombok.Getter;

public abstract class FormController<V extends FormView<?, ?>, M extends FormModel, P extends CryptoAssetAccountPayload> implements Controller {
    @Getter
    protected final V view;
    protected final M model;

    protected FormController(ServiceProvider serviceProvider, DigitalAssetPaymentMethod paymentMethod) {
        this.model = createModel(paymentMethod);
        this.view = createView();
    }

    protected abstract V createView();

    protected abstract M createModel(DigitalAssetPaymentMethod paymentMethod);

    public abstract P createAccountPayload();

    @Override
    public void onActivate() {
        model.getRunValidation().set(false);
    }

    @Override
    public void onDeactivate() {
    }

    void onValidationDone() {
        model.getRunValidation().set(false);
    }

    public boolean validate() {
        boolean isValid = model.getAddressValidator().validateAndGet();
        model.getRunValidation().set(true);
        return isValid;
    }

    void onIsInstantToggled(boolean selected) {
        model.getIsInstant().set(!model.getIsInstant().get());
    }

    void onIsAutoConfToggled(boolean selected) {
        model.getIsAutoConf().set(!model.getIsAutoConf().get());
    }
}