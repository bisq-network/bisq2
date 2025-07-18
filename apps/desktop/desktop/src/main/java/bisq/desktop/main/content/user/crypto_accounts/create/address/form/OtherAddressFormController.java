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

import bisq.account.accounts.crypto.OtherCryptoAssetAccountPayload;
import bisq.account.payment_method.CryptoPaymentMethod;
import bisq.common.monetary.Monetary;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.presentation.parser.AmountParser;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class OtherAddressFormController extends AddressFormController<OtherAddressFormView, OtherAddressFormModel, OtherCryptoAssetAccountPayload> {
    public OtherAddressFormController(ServiceProvider serviceProvider, CryptoPaymentMethod paymentMethod) {
        super(serviceProvider, paymentMethod);
    }

    @Override
    protected OtherAddressFormView createView() {
        return new OtherAddressFormView(model, this);
    }

    @Override
    protected OtherAddressFormModel createModel(CryptoPaymentMethod paymentMethod) {
        return new OtherAddressFormModel(StringUtils.createUid(), paymentMethod);
    }

    @Override
    public OtherCryptoAssetAccountPayload createAccountPayload() {
        Optional<Boolean> isAutoConf = model.isAutoConfSupported() ? Optional.of(model.getIsAutoConf().get()) : Optional.empty();
        Optional<Integer> autoConfNumConfirmations = model.getAutoConfNumConfirmations().get() == null
                ? Optional.empty()
                : Optional.of(Integer.valueOf(model.getAutoConfNumConfirmations().get()));
        Optional<Long> autoConfMaxTradeAmount = Optional.empty();
        if (model.getAutoConfMaxTradeAmount().get() != null) {
            Monetary amount = AmountParser.parse(model.getAutoConfMaxTradeAmount().get(), "BTC");
            autoConfMaxTradeAmount = Optional.of(amount.getValue());
        }
        return new OtherCryptoAssetAccountPayload(model.getId(),
                model.getCurrencyCode(),
                model.getAddress().get(),
                model.getIsInstant().get(),
                isAutoConf,
                autoConfNumConfirmations,
                autoConfMaxTradeAmount,
                Optional.ofNullable(model.getAutoConfExplorerUrls().get())
        );
    }
}