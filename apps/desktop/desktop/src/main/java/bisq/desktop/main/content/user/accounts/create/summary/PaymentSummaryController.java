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

package bisq.desktop.main.content.user.accounts.create.summary;

import bisq.account.AccountService;
import bisq.account.accounts.Account;
import bisq.account.accounts.AccountPayload;
import bisq.account.accounts.CountryBasedAccountPayload;
import bisq.account.accounts.F2FAccount;
import bisq.account.accounts.F2FAccountPayload;
import bisq.account.accounts.FasterPaymentsAccount;
import bisq.account.accounts.FasterPaymentsAccountPayload;
import bisq.account.accounts.MultiCurrencyAccountPayload;
import bisq.account.accounts.NationalBankAccount;
import bisq.account.accounts.NationalBankAccountPayload;
import bisq.account.accounts.PixAccount;
import bisq.account.accounts.PixAccountPayload;
import bisq.account.accounts.RevolutAccount;
import bisq.account.accounts.RevolutAccountPayload;
import bisq.account.accounts.SepaAccount;
import bisq.account.accounts.SepaAccountPayload;
import bisq.account.accounts.ZelleAccount;
import bisq.account.accounts.ZelleAccountPayload;
import bisq.account.payment_method.CryptoPaymentRail;
import bisq.account.payment_method.FiatPaymentRail;
import bisq.account.payment_method.PaymentMethod;
import bisq.common.currency.FiatCurrencyRepository;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.user.accounts.create.summary.details.AccountDetailsGridPane;
import bisq.desktop.main.content.user.accounts.create.summary.details.F2FAccountDetailsGridPane;
import bisq.desktop.main.content.user.accounts.create.summary.details.FasterPaymentsAccountDetailsGridPane;
import bisq.desktop.main.content.user.accounts.create.summary.details.NationalBankAccountDetailsGridPane;
import bisq.desktop.main.content.user.accounts.create.summary.details.PixAccountDetailsGridPane;
import bisq.desktop.main.content.user.accounts.create.summary.details.RevolutAccountDetailsGridPane;
import bisq.desktop.main.content.user.accounts.create.summary.details.SepaAccountDetailsGridPane;
import bisq.desktop.main.content.user.accounts.create.summary.details.ZelleAccountDetailsGridPane;
import bisq.desktop.overlay.OverlayController;
import bisq.i18n.Res;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.Set;
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

    public void setPaymentMethod(PaymentMethod<?> paymentMethod) {
        checkNotNull(paymentMethod, "PaymentMethod must not be null");
        model.setPaymentMethod(paymentMethod);
    }

    public void setAccountPayload(AccountPayload<?> accountPayload) {
        model.setAccountPayload(accountPayload);
        model.setDefaultAccountName(accountPayload.getDefaultAccountName());

        if (accountPayload instanceof MultiCurrencyAccountPayload multiCurrencyAccountPayload) {
            model.setCurrency(FiatCurrencyRepository.getCodeAndDisplayNames(multiCurrencyAccountPayload.getSelectedCurrencyCodes()));
        } else {
            model.setCurrency(FiatCurrencyRepository.getCodeAndDisplayName(accountPayload.getCurrencyCode().orElseThrow()));
        }

        if (accountPayload instanceof CountryBasedAccountPayload countryBasedAccountPayload) {
            model.setCountry(countryBasedAccountPayload.getCountry().getName());
        }

        if (model.getPaymentMethod().getPaymentRail() instanceof FiatPaymentRail fiatPaymentRail) {
            AccountDetailsGridPane<?, ?> accountDetailsGridPane = getAccountDetailsGridPane(accountPayload, fiatPaymentRail);
            model.setAccountDetailsGridPane(accountDetailsGridPane);
        }
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
            new Popup().warning(Res.get("user.paymentAccounts.summary.accountNameOverlay.accountName.error.nameAlreadyExists")).show();
            return;
        }

        Account<? extends PaymentMethod<?>, ?> account = getAccount(accountName);
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

    private AccountDetailsGridPane<?, ?> getAccountDetailsGridPane(AccountPayload<?> accountPayload,
                                                                   FiatPaymentRail fiatPaymentRail) {
        return switch (fiatPaymentRail) {
            case CUSTOM -> throw new UnsupportedOperationException("FiatPaymentRail.CUSTOM is not supported");
            case SEPA -> new SepaAccountDetailsGridPane((SepaAccountPayload) accountPayload, fiatPaymentRail);
            case SEPA_INSTANT -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
            case ZELLE -> new ZelleAccountDetailsGridPane((ZelleAccountPayload) accountPayload, fiatPaymentRail);
            case REVOLUT -> new RevolutAccountDetailsGridPane((RevolutAccountPayload) accountPayload, fiatPaymentRail);
            case WISE -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
            case NATIONAL_BANK ->
                    new NationalBankAccountDetailsGridPane((NationalBankAccountPayload) accountPayload, fiatPaymentRail);
            case SAME_BANK -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
            case SWIFT -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
            case F2F -> new F2FAccountDetailsGridPane((F2FAccountPayload) accountPayload, fiatPaymentRail);
            case ACH_TRANSFER -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
            case PIX -> new PixAccountDetailsGridPane((PixAccountPayload) accountPayload, fiatPaymentRail);
            case FASTER_PAYMENTS ->
                    new FasterPaymentsAccountDetailsGridPane((FasterPaymentsAccountPayload) accountPayload, fiatPaymentRail);
            case PAY_ID -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
            case US_POSTAL_MONEY_ORDER ->
                    throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
            case CASH_BY_MAIL -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
            case STRIKE -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
            case INTERAC_E_TRANSFER ->
                    throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
            case AMAZON_GIFT_CARD ->
                    throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
            case CASH_DEPOSIT -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
            case UPI -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
            case BIZUM -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
            case CASH_APP -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
            case DOMESTIC_WIRE_TRANSFER ->
                    throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
        };
    }

    private Account<? extends PaymentMethod<?>, ?> getAccount(String accountName) {
        if (model.getPaymentMethod().getPaymentRail() instanceof FiatPaymentRail fiatPaymentRail) {
            return switch (fiatPaymentRail) {
                case CUSTOM -> throw new UnsupportedOperationException("FiatPaymentRail.CUSTOM is not supported");
                case SEPA ->
                        new SepaAccount(StringUtils.createUid(), new Date().getTime(), accountName, (SepaAccountPayload) model.getAccountPayload());
                case SEPA_INSTANT ->
                        throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
                case ZELLE ->
                        new ZelleAccount(StringUtils.createUid(), new Date().getTime(), accountName, (ZelleAccountPayload) model.getAccountPayload());
                case REVOLUT ->
                        new RevolutAccount(StringUtils.createUid(), new Date().getTime(), accountName, (RevolutAccountPayload) model.getAccountPayload());
                case WISE -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
                case NATIONAL_BANK ->
                        new NationalBankAccount(StringUtils.createUid(), new Date().getTime(), accountName, (NationalBankAccountPayload) model.getAccountPayload());
                case SAME_BANK -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
                case SWIFT -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
                case F2F ->
                        new F2FAccount(StringUtils.createUid(), new Date().getTime(), accountName, (F2FAccountPayload) model.getAccountPayload());
                case ACH_TRANSFER ->
                        throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
                case PIX ->
                        new PixAccount(StringUtils.createUid(), new Date().getTime(), accountName, (PixAccountPayload) model.getAccountPayload());
                case FASTER_PAYMENTS ->
                        new FasterPaymentsAccount(StringUtils.createUid(), new Date().getTime(), accountName, (FasterPaymentsAccountPayload) model.getAccountPayload());
                case PAY_ID -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
                case US_POSTAL_MONEY_ORDER ->
                        throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
                case CASH_BY_MAIL ->
                        throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
                case STRIKE -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
                case INTERAC_E_TRANSFER ->
                        throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
                case AMAZON_GIFT_CARD ->
                        throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
                case CASH_DEPOSIT ->
                        throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
                case UPI -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
                case BIZUM -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
                case CASH_APP -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
                case DOMESTIC_WIRE_TRANSFER ->
                        throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
            };
        } else if (model.getPaymentMethod().getPaymentRail() instanceof CryptoPaymentRail cryptoPaymentRail) {
            throw new UnsupportedOperationException("Unsupported paymentRail " + model.getPaymentMethod().getPaymentRail());
            /*
            return switch (cryptoPaymentRail) {
                case CUSTOM -> throw new UnsupportedOperationException("Not yet implemented:  " + cryptoPaymentRail);
                case BSQ -> throw new UnsupportedOperationException("Not yet implemented:  " + cryptoPaymentRail);
                case MONERO -> throw new UnsupportedOperationException("Not yet implemented:  " + cryptoPaymentRail);
                case LIQUID -> throw new UnsupportedOperationException("Not yet implemented:  " + cryptoPaymentRail);
                case NATIVE_CHAIN -> throw new UnsupportedOperationException("Not yet implemented:  " + cryptoPaymentRail);
                case ATOMIC_SWAP_CAPABLE_CHAIN -> throw new UnsupportedOperationException("Not yet implemented:  " + cryptoPaymentRail);
                case OTHER -> throw new UnsupportedOperationException("Not yet implemented:  " + cryptoPaymentRail);
            };*/
        } else {
            throw new UnsupportedOperationException("Unsupported paymentRail " + model.getPaymentMethod().getPaymentRail());
        }
    }
}