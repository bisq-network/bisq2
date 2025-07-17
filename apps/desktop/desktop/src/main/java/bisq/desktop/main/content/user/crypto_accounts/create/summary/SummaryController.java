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

package bisq.desktop.main.content.user.crypto_accounts.create.summary;

import bisq.account.AccountService;
import bisq.account.accounts.Account;
import bisq.account.accounts.crypto.CryptoAssetAccountPayload;
import bisq.account.accounts.crypto.MoneroAccount;
import bisq.account.accounts.crypto.MoneroAccountPayload;
import bisq.account.accounts.crypto.OtherCryptoAssetAccount;
import bisq.account.accounts.crypto.OtherCryptoAssetAccountPayload;
import bisq.account.payment_method.CryptoPaymentMethod;
import bisq.account.payment_method.PaymentMethod;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.user.crypto_accounts.create.summary.details.MoneroSummaryDetails;
import bisq.desktop.main.content.user.crypto_accounts.create.summary.details.SummaryDetails;
import bisq.desktop.overlay.OverlayController;
import bisq.i18n.Res;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class SummaryController implements Controller {
    private final SummaryModel model;
    @Getter
    private final SummaryView view;
    private final AccountService accountService;

    public SummaryController(ServiceProvider serviceProvider) {
        accountService = serviceProvider.getAccountService();
        model = new SummaryModel();
        view = new SummaryView(model, this);
    }

    public void setPaymentMethod(CryptoPaymentMethod paymentMethod) {
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

        Account<? extends PaymentMethod<?>, ?> account;
        CryptoAssetAccountPayload accountPayload = model.getAccountPayload();
        if (accountPayload instanceof MoneroAccountPayload moneroAccountPayload) {
            account = new MoneroAccount(StringUtils.createUid(),
                    new Date().getTime(),
                    accountName, moneroAccountPayload);
        } else if (accountPayload instanceof OtherCryptoAssetAccountPayload otherCryptoAssetAccountPayload) {
            account = new OtherCryptoAssetAccount(StringUtils.createUid(),
                    new Date().getTime(),
                    accountName, otherCryptoAssetAccountPayload);
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