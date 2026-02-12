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

package bisq.desktop.main.content.user.accounts.crypto_accounts.create.data;

import bisq.account.accounts.crypto.CryptoAssetAccountPayload;
import bisq.account.payment_method.DigitalAssetPaymentMethod;
import bisq.account.payment_method.cbdc.CbdcPaymentMethod;
import bisq.account.payment_method.stable_coin.StableCoinPaymentMethod;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.user.accounts.crypto_accounts.create.data.form.FormController;
import bisq.desktop.main.content.user.accounts.crypto_accounts.create.data.form.MoneroFormController;
import bisq.desktop.main.content.user.accounts.crypto_accounts.create.data.form.OtherFormController;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AccountDataController implements Controller {
    private final AccountDataModel model;
    @Getter
    private final AccountDataView view;
    private final ServiceProvider serviceProvider;
    private FormController<?, ?, ?> formController;

    public AccountDataController(ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;

        model = new AccountDataModel();
        view = new AccountDataView(model, this);
    }

    public CryptoAssetAccountPayload getAccountPayload() {
        return formController.createAccountPayload();
    }

    public void setPaymentMethod(DigitalAssetPaymentMethod paymentMethod) {
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

    public FormController<?, ?, ?> getOrCreateController(DigitalAssetPaymentMethod paymentMethod) {
        return model.getControllerCache().computeIfAbsent(paymentMethod.getId(), k -> createController(paymentMethod));
    }

    public FormController<?, ?, ?> createController(DigitalAssetPaymentMethod paymentMethod) {
        if (paymentMethod instanceof DigitalAssetPaymentMethod cryptoPaymentMethod) {
            if (cryptoPaymentMethod.getCode().equals("XMR")) {
                return new MoneroFormController(serviceProvider, cryptoPaymentMethod);
            } else {
                return new OtherFormController(serviceProvider, cryptoPaymentMethod);
            }
        } else if (paymentMethod instanceof StableCoinPaymentMethod stableCoinPaymentMethod) {
            throw new RuntimeException("Not impl yet");
        } else if (paymentMethod instanceof CbdcPaymentMethod cbdcPaymentMethod) {
            throw new RuntimeException("Not impl yet");
        } else {
            throw new UnsupportedOperationException("PaymentMethod not supported");
        }
    }
}