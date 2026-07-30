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

import bisq.account.accounts.crypto.monero.MoneroAccountPayload;
import bisq.account.payment_method.DigitalAssetPaymentMethod;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.presentation.parser.AmountParser;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import bisq.account.accounts.crypto.monero.knaccc.monero.address.WalletAddress;

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

    // Bisq 1 reference: bisq.core.payment.XmrAccountDelegate.createAndSetNewSubAddress
    private String createSubAddress(String mainAddress,
                                    String privateViewKey,
                                    String accountIndex,
                                    String initialSubAddressIndex) {
        if (StringUtils.isEmpty(mainAddress) || StringUtils.isEmpty(privateViewKey)) {
            return "";
        }

        long accountIndexValue;
        long subAddressIndexValue;
        try {
            accountIndexValue = StringUtils.isEmpty(accountIndex) ? 0L : Long.parseLong(accountIndex);
            subAddressIndexValue = StringUtils.isEmpty(initialSubAddressIndex) ? 0L : Long.parseLong(initialSubAddressIndex);
        } catch (NumberFormatException e) {
            log.error("Invalid Monero account/subaddress index", e);
            return "";
        }

        try {
            checkArgument(accountIndexValue >= 0, "accountIndex must be >= 0, was: %s", accountIndexValue);
            checkArgument(subAddressIndexValue >= 0, "subAddressIndex must be >= 0, was: %s", subAddressIndexValue);
            checkArgument(accountIndexValue > 0 || subAddressIndexValue > 0,
                    "accountIndex and subAddressIndex cannot both be 0 (would represent main address)");

            WalletAddress walletAddress = new WalletAddress(mainAddress);
            long start = System.currentTimeMillis();
            String subAddress = walletAddress.getSubaddressBase58(privateViewKey, accountIndexValue, subAddressIndexValue);
            log.info("Created new subAddress {}. Took {} ms.", subAddress, System.currentTimeMillis() - start);
            return subAddress;
        } catch (IllegalArgumentException e) {
            log.error("Monero subaddress validation failed", e);
            return "";
        } catch (WalletAddress.InvalidWalletAddressException e) {
            log.error("WalletAddress.getSubaddressBase58 failed", e);
            return "";
        } catch (RuntimeException e) {
            log.error("Unexpected error during subaddress generation", e);
            return "";
        }
    }
}