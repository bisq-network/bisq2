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

package bisq.desktop.main.content.user.crypto_accounts.create.address.form;

import bisq.account.accounts.crypto.MoneroAccountPayload;
import bisq.account.payment_method.CryptoPaymentMethod;
import bisq.common.monetary.Monetary;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.presentation.parser.AmountParser;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class MoneroAddressFormController extends AddressFormController<MoneroAddressFormView, MoneroAddressFormModel, MoneroAccountPayload> {
    private final CryptoPaymentMethod paymentMethod;

    public MoneroAddressFormController(ServiceProvider serviceProvider, CryptoPaymentMethod paymentMethod) {
        super(serviceProvider, paymentMethod);
        this.paymentMethod = paymentMethod;
    }

    @Override
    protected MoneroAddressFormView createView() {
        return new MoneroAddressFormView(model, this);
    }

    @Override
    protected MoneroAddressFormModel createModel(CryptoPaymentMethod paymentMethod) {
        return new MoneroAddressFormModel(StringUtils.createUid(), paymentMethod);
    }

    @Override
    public MoneroAccountPayload createAccountPayload() {
        Optional<Boolean> isAutoConf = model.isAutoConfSupported() ? Optional.of(model.getIsAutoConf().get()) : Optional.empty();
        Optional<Integer> autoConfNumConfirmations = model.getAutoConfNumConfirmations().get() == null
                ? Optional.empty()
                : Optional.of(Integer.valueOf(model.getAutoConfNumConfirmations().get()));
        Optional<Long> autoConfMaxTradeAmount = Optional.empty();
        if (model.getAutoConfMaxTradeAmount().get() != null) {
            Monetary amount = AmountParser.parse(model.getAutoConfMaxTradeAmount().get(), "BTC");
            autoConfMaxTradeAmount = Optional.of(amount.getValue());
        }
        Optional<Integer> accountIndex = model.getAccountIndex().get() == null
                ? Optional.empty()
                : Optional.of(Integer.valueOf(model.getAccountIndex().get()));
        Optional<Integer> initialSubAddressIndex = model.getInitialSubAddressIndex().get() == null
                ? Optional.empty()
                : Optional.of(Integer.valueOf(model.getInitialSubAddressIndex().get()));
        return new MoneroAccountPayload(model.getId(),
                model.getAddress().get(),
                model.getIsInstant().get(),
                isAutoConf,
                autoConfNumConfirmations,
                autoConfMaxTradeAmount,
                Optional.ofNullable(model.getAutoConfExplorerUrls().get()),
                model.getUseSubAddresses().get(),
                Optional.ofNullable(model.getPrivateViewKey().get()),
                accountIndex,
                initialSubAddressIndex
        );
    }

    void onUseSubAddressesToggled(boolean selected) {
        model.getUseSubAddresses().set(!model.getUseSubAddresses().get());
    }
}