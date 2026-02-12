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

import bisq.account.accounts.crypto.MoneroAccountPayload;
import bisq.account.payment_method.DigitalAssetPaymentMethod;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.presentation.parser.AmountParser;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;

@Slf4j
public class MoneroFormController extends FormController<MoneroFormView, MoneroFormModel, MoneroAccountPayload> {
    private Subscription createSubAddressPin;

    public MoneroFormController(ServiceProvider serviceProvider, DigitalAssetPaymentMethod paymentMethod) {
        super(serviceProvider, paymentMethod);
    }

    @Override
    public void onActivate() {
        super.onActivate();

        createSubAddressPin = EasyBind.subscribe(EasyBind.combine(model.getMainAddress(),
                        model.getPrivateViewKey(),
                        model.getAccountIndex(),
                        model.getInitialSubAddressIndex(),
                        this::createSubAddress),
                subAddress -> model.getSubAddress().set(subAddress));
    }

    @Override
    public void onDeactivate() {
        super.onDeactivate();

        createSubAddressPin.unsubscribe();

    }

    @Override
    protected MoneroFormView createView() {
        return new MoneroFormView(model, this);
    }

    @Override
    protected MoneroFormModel createModel(DigitalAssetPaymentMethod paymentMethod) {
        return new MoneroFormModel(StringUtils.createUid(), paymentMethod);
    }

    @Override
    public MoneroAccountPayload createAccountPayload() {
        Optional<Boolean> isAutoConf = model.isAutoConfSupported() ? Optional.of(model.getIsAutoConf().get()) : Optional.empty();
        Optional<Integer> autoConfNumConfirmations = Optional.ofNullable(model.getAutoConfNumConfirmations().get())
                .map(Integer::valueOf);
        Optional<Long> autoConfMaxTradeAmount = Optional.ofNullable(model.getAutoConfMaxTradeAmount().get())
                .map(e -> AmountParser.parse(e, "BTC").getValue());
        Optional<String> autoConfExplorerUrls = Optional.ofNullable(model.getAutoConfExplorerUrls().get());

        boolean useSubAddresses = model.getUseSubAddresses().get();
        Optional<String> mainAddress = Optional.ofNullable(model.getMainAddress().get());
        Optional<String> privateViewKey = Optional.ofNullable(model.getPrivateViewKey().get());
        Optional<String> subAddress = Optional.ofNullable(model.getSubAddress().get());
        Optional<Integer> accountIndex = Optional.ofNullable(model.getAccountIndex().get())
                .map(Integer::valueOf);
        Optional<Integer> initialSubAddressIndex = Optional.ofNullable(model.getInitialSubAddressIndex().get())
                .map(Integer::valueOf);

        return new MoneroAccountPayload(model.getId(),
                model.getAddress().get(),
                model.getIsInstant().get(),
                isAutoConf,
                autoConfNumConfirmations,
                autoConfMaxTradeAmount,
                autoConfExplorerUrls,
                useSubAddresses,
                mainAddress,
                privateViewKey,
                subAddress,
                accountIndex,
                initialSubAddressIndex
        );
    }

    void onUseSubAddressesToggled(boolean selected) {
        model.getUseSubAddresses().set(selected);
    }

    // TODO impl. following Bisq 1 `bisq.core.payment.XmrAccountDelegate.createAndSetNewSubAddress`
    private String createSubAddress(String mainAddress,
                                    String privateViewKey,
                                    String accountIndex,
                                    String initialSubAddressIndex) {
        return "TODO: SubAddress creation not implemented yet";
    }

}