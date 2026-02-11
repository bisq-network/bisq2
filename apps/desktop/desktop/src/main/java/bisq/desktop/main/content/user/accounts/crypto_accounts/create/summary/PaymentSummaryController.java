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

package bisq.desktop.main.content.user.accounts.crypto_accounts.create.summary;

import bisq.account.AccountService;
import bisq.account.accounts.Account;
import bisq.account.accounts.crypto.CryptoAssetAccountPayload;
import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.crypto.MoneroAccount;
import bisq.account.accounts.crypto.MoneroAccountPayload;
import bisq.account.accounts.crypto.OtherCryptoAssetAccount;
import bisq.account.accounts.crypto.OtherCryptoAssetAccountPayload;
import bisq.account.timestamp.KeyAlgorithm;
import bisq.account.payment_method.DigitalAssetPaymentMethod;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.user.accounts.crypto_accounts.create.summary.details.MoneroSummaryDetails;
import bisq.desktop.main.content.user.accounts.crypto_accounts.create.summary.details.SummaryDetails;
import bisq.desktop.overlay.OverlayController;
import bisq.i18n.Res;
import bisq.security.keys.KeyGeneration;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.security.KeyPair;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class PaymentSummaryController implements Controller {
    private final PaymentSummaryModel model;
    @Getter
    private final PaymentSummaryView view;
    private final AccountService accountService;

    public PaymentSummaryController(ServiceProvider serviceProvider) {
        accountService = serviceProvider.getAccountService();
        model = new PaymentSummaryModel();
        view = new PaymentSummaryView(model, this);
    }

    public void setPaymentMethod(DigitalAssetPaymentMethod paymentMethod) {
        checkNotNull(paymentMethod, "PaymentMethod must not be null");
        model.setPaymentMethod(paymentMethod);
    }

    public void setAccountPayload(CryptoAssetAccountPayload accountPayload) {
        model.setAccountPayload(accountPayload);
        model.setDefaultAccountName(accountPayload.getDefaultAccountName());
        model.setCurrencyString(accountPayload.getCodeAndDisplayName());

        if (accountPayload instanceof MoneroAccountPayload moneroAccountPayload && moneroAccountPayload.isUseSubAddresses()) {
            model.setAddressDescription(Res.get("paymentAccounts.crypto.address.xmr.mainAddresses"));
        } else {
            model.setAddressDescription(Res.get("paymentAccounts.crypto.address.address"));
        }

        SummaryDetails<?> accountDetailsGridPane;
        if (accountPayload instanceof MoneroAccountPayload moneroAccountPayload) {
            accountDetailsGridPane = new MoneroSummaryDetails(moneroAccountPayload);
        } else {
            accountDetailsGridPane = new SummaryDetails<>(accountPayload);
        }
        model.setSummaryDetails(accountDetailsGridPane);
    }

    public void showAccountNameOverlay() {
        model.getShowAccountNameOverlay().set(true);
    }

    public void onCreateAccount(String accountName) {
        if (model.getAccountNameValidator().hasErrors()) {
            new Popup().invalid(model.getAccountNameValidator().getMessage())
                    .owner(view.getRoot())
                    .show();
            return;
        }
        Set<String> existingNames = accountService.getAccounts().stream()
                .map(Account::getAccountName)
                .collect(Collectors.toSet());
        if (existingNames.contains(accountName)) {
            new Popup().warning(Res.get("paymentAccounts.summary.accountNameOverlay.accountName.error.nameAlreadyExists")).show();
            return;
        }

        Account<? extends DigitalAssetPaymentMethod, ?> account;
        CryptoAssetAccountPayload accountPayload = model.getAccountPayload();
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        KeyAlgorithm keyAlgorithm = KeyAlgorithm.EC;
        if (accountPayload instanceof MoneroAccountPayload moneroAccountPayload) {
            account = new MoneroAccount(StringUtils.createUid(),
                    System.currentTimeMillis(),
                    accountName,
                    moneroAccountPayload,
                    keyPair,
                    keyAlgorithm,
                    AccountOrigin.BISQ2_NEW);
        } else if (accountPayload instanceof OtherCryptoAssetAccountPayload otherCryptoAssetAccountPayload) {
            account = new OtherCryptoAssetAccount(StringUtils.createUid(),
                    System.currentTimeMillis(),
                    accountName,
                    otherCryptoAssetAccountPayload,
                    keyPair,
                    keyAlgorithm,
                    AccountOrigin.BISQ2_NEW);
        } else {
            throw new UnsupportedOperationException("Unsupported accountPayload " + accountPayload.getClass().getSimpleName());
        }

        accountService.addPaymentAccount(account);
        model.getShowAccountNameOverlay().set(false);
        OverlayController.hide();
    }

    @Override
    public void onActivate() {
        model.getShowAccountNameOverlay().set(false);
    }

    @Override
    public void onDeactivate() {
    }
}
