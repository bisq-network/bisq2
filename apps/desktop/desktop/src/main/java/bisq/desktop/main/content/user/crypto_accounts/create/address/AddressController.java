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

package bisq.desktop.main.content.user.crypto_accounts.create.address;

import bisq.account.accounts.crypto.CryptoAssetAccountPayload;
import bisq.account.payment_method.crypto.CryptoPaymentMethod;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.user.crypto_accounts.create.address.form.AddressFormController;
import bisq.desktop.main.content.user.crypto_accounts.create.address.form.MoneroAddressFormController;
import bisq.desktop.main.content.user.crypto_accounts.create.address.form.OtherAddressFormController;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AddressController implements Controller {
    private final AddressModel model;
    @Getter
    private final AddressView view;
    private final ServiceProvider serviceProvider;
    private AddressFormController<?, ?, ?> formController;

    public AddressController(ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;

        model = new AddressModel();
        view = new AddressView(model, this);
    }

    public CryptoAssetAccountPayload getAccountPayload() {
        return formController.createAccountPayload();
    }

    public void setPaymentMethod(CryptoPaymentMethod paymentMethod) {
        model.setPaymentMethod(paymentMethod);

        formController = getOrCreateController(paymentMethod);
        model.setPaymentForm(formController.getView().getRoot());
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    public boolean validate() {
        return formController != null && formController.validate();
    }

    public AddressFormController<?, ?, ?> getOrCreateController(CryptoPaymentMethod paymentMethod) {
        String currencyCode = paymentMethod.getCode();
        return model.getControllerCache().computeIfAbsent(currencyCode, k -> createController(paymentMethod));
    }

    public AddressFormController<?, ?, ?> createController(CryptoPaymentMethod paymentMethod) {
        if (paymentMethod.getCode().equals("XMR")) {
            return new MoneroAddressFormController(serviceProvider, paymentMethod);
        } else {
            return new OtherAddressFormController(serviceProvider, paymentMethod);
        }
    }
}