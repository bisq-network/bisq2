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

package bisq.desktop.main.content.user.fiat_accounts;

import bisq.account.AccountService;
import bisq.account.accounts.Account;
import bisq.account.accounts.AccountPayload;
import bisq.account.accounts.crypto.CryptoAssetAccount;
import bisq.account.accounts.fiat.F2FAccount;
import bisq.account.accounts.fiat.FasterPaymentsAccount;
import bisq.account.accounts.fiat.NationalBankAccount;
import bisq.account.accounts.fiat.PixAccount;
import bisq.account.accounts.fiat.RevolutAccount;
import bisq.account.accounts.fiat.SepaAccount;
import bisq.account.accounts.fiat.UserDefinedFiatAccount;
import bisq.account.accounts.fiat.UserDefinedFiatAccountPayload;
import bisq.account.accounts.fiat.ZelleAccount;
import bisq.account.payment_method.PaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.common.observable.Pin;
import bisq.common.observable.map.HashMapObserver;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.FileChooserUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.user.fiat_accounts.details.AccountDetails;
import bisq.desktop.main.content.user.fiat_accounts.details.F2FAccountDetails;
import bisq.desktop.main.content.user.fiat_accounts.details.FasterPaymentsAccountDetails;
import bisq.desktop.main.content.user.fiat_accounts.details.NationalBankAccountDetails;
import bisq.desktop.main.content.user.fiat_accounts.details.PixAccountDetails;
import bisq.desktop.main.content.user.fiat_accounts.details.RevolutAccountDetails;
import bisq.desktop.main.content.user.fiat_accounts.details.SepaAccountDetails;
import bisq.desktop.main.content.user.fiat_accounts.details.UserDefinedAccountDetails;
import bisq.desktop.main.content.user.fiat_accounts.details.ZelleAccountDetails;
import bisq.desktop.navigation.NavigationTarget;
import bisq.mu_sig.MuSigService;
import javafx.beans.property.ReadOnlyStringProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import javax.annotation.Nullable;
import java.nio.file.Files;
import java.util.Comparator;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class FiatPaymentAccountsController implements Controller {
    private final FiatPaymentAccountsModel model;
    @Getter
    private final FiatPaymentAccountsView view;
    private final AccountService accountService;
    private final MuSigService muSigService;
    private Subscription selectedAccountSubscription;
    private Pin accountsPin, selectedAccountPin;
    @Nullable
    private Subscription userDefinedAccountDetailsPin;

    public FiatPaymentAccountsController(ServiceProvider serviceProvider) {
        accountService = serviceProvider.getAccountService();
        muSigService = serviceProvider.getMuSigService();

        model = new FiatPaymentAccountsModel();
        view = new FiatPaymentAccountsView(model, this);

        model.getSortedAccounts().setComparator(Comparator.comparing(Account::getAccountName));
    }

    @Override
    public void onActivate() {
        accountsPin = accountService.getAccountByNameMap().addObserver(new HashMapObserver<>() {
            @Override
            public void put(String key, Account<? extends PaymentMethod<?>, ?> account) {
                UIThread.run(() -> {
                    if (!(account instanceof CryptoAssetAccount) && !model.getAccounts().contains(account)) {
                        model.getAccounts().add(account);
                        accountService.setSelectedAccount(account);
                        updateNoAccountsState();
                    }
                });
            }

            @Override
            public void remove(Object element) {
                if (element instanceof Account<? extends PaymentMethod<?>, ?> account) {
                    UIThread.run(() -> {
                        model.getAccounts().remove(account);
                        maybeSelectFirstAccount();
                        updateNoAccountsState();
                    });
                }
            }

            @Override
            public void clear() {
                UIThread.run(() -> {
                    model.getAccounts().clear();
                    updateNoAccountsState();
                });
            }
        });

        selectedAccountPin = FxBindings.bind(model.getSelectedAccount())
                .to(accountService.selectedAccountAsObservable());

        selectedAccountSubscription = EasyBind.subscribe(model.getSelectedAccount(),
                selectedAccount -> {
                    accountService.setSelectedAccount(selectedAccount);
                    applyDataDisplay(selectedAccount);

                    if (UserDefinedAccountDetails.USE_LEGACY_DESIGN) {
                        disposeUserDefinedAccountDetailsPin();
                        if (UserDefinedAccountDetails.USE_LEGACY_DESIGN && selectedAccount instanceof UserDefinedFiatAccount userDefinedFiatAccount) {
                            AccountDetails<?, ?> accountDetails = model.getAccountDetails().get();
                            if (accountDetails instanceof UserDefinedAccountDetails userDefinedAccountDetails) {
                                ReadOnlyStringProperty textAreaTextProperty = userDefinedAccountDetails.getTextAreaTextProperty();
                                if (textAreaTextProperty != null) {
                                    String accountData = textAreaTextProperty.get();
                                    userDefinedAccountDetailsPin = EasyBind.subscribe(textAreaTextProperty, newValue -> {
                                        String oldValue = userDefinedFiatAccount.getAccountPayload().getAccountData();
                                        model.getSaveButtonDisabled().set(StringUtils.isEmpty(newValue) || StringUtils.isEmpty(oldValue) || oldValue.equals(newValue));
                                    });
                                }
                            }
                        }
                    }
                    updateDeleteButtonStates();
                });

        maybeSelectFirstAccount();
        updateNoAccountsState();
    }

    @Override
    public void onDeactivate() {
        accountsPin.unbind();
        selectedAccountPin.unbind();
        selectedAccountSubscription.unsubscribe();
        disposeUserDefinedAccountDetailsPin();
        model.getAccounts().clear();
    }

    void onSelectAccount(Account<? extends PaymentMethod<?>, ?> account) {
        if (account != null) {
            accountService.setSelectedAccount(account);
            model.getSelectedAccount().set(account);
        }
    }

    void onCreateAccount() {
        if (muSigService.getMuSigActivated().get()) {
            Navigation.navigateTo(NavigationTarget.CREATE_PAYMENT_ACCOUNT);
        } else {
            Navigation.navigateTo(NavigationTarget.CREATE_PAYMENT_ACCOUNT_LEGACY);
        }
    }

    void onSaveAccount() {
        var selectedAccount = model.getSelectedAccount().get();
        if (selectedAccount instanceof UserDefinedFiatAccount userDefinedFiatAccount) {
            AccountDetails<?, ?> accountDetails = model.getAccountDetails().get();
            if (accountDetails instanceof UserDefinedAccountDetails userDefinedAccountDetails) {
                ReadOnlyStringProperty textAreaTextProperty = userDefinedAccountDetails.getTextAreaTextProperty();
                String accountData = textAreaTextProperty != null ? textAreaTextProperty.get() : null;
                if (StringUtils.isEmpty(accountData)) {
                    return;
                }
                checkArgument(accountData.length() <= UserDefinedFiatAccountPayload.MAX_DATA_LENGTH, "Account data must not be longer than 1000 characters");

                String accountId = userDefinedFiatAccount.getId();
                long creationDate = userDefinedFiatAccount.getCreationDate();
                String accountName = userDefinedFiatAccount.getAccountName();
                UserDefinedFiatAccountPayload newAccountPayload = new UserDefinedFiatAccountPayload(accountId, accountData);
                UserDefinedFiatAccount newAccount = new UserDefinedFiatAccount(accountId,
                        creationDate,
                        accountName,
                        newAccountPayload);
                accountService.removePaymentAccount(selectedAccount);
                accountService.addPaymentAccount(newAccount);
                accountService.setSelectedAccount(newAccount);
            }
        }
    }

    void onDeleteAccount() {
        Account<? extends PaymentMethod<?>, ?> account = model.getSelectedAccount().get();
        accountService.removePaymentAccount(account);
        model.getAccounts().remove(account);
        maybeSelectFirstAccount();
    }

    void onImportBisq1AccountData() {
        FileChooserUtil.openFile(getView().getRoot().getScene())
                .ifPresent(path -> {
                    try {
                        String json = Files.readString(path);
                        checkArgument(StringUtils.isNotEmpty(json), "Json must not be empty");
                        accountService.importBisq1AccountData(json);
                    } catch (Exception e) {
                        new Popup().error(e).show();
                    }
                });
    }

    private void updateDeleteButtonStates() {
        model.getDeleteButtonDisabled().set(model.getSelectedAccount().get() == null);
    }

    private void updateNoAccountsState() {
        model.getNoAccountsAvailable().set(!accountService.hasAccounts());
        if (UserDefinedAccountDetails.USE_LEGACY_DESIGN) {
            model.getSaveButtonVisible().set(accountService.hasAccounts());
        }
    }

    private void maybeSelectFirstAccount() {
        if (accountService.getSelectedAccount().isEmpty() && !model.getSortedAccounts().isEmpty()) {
            Account<? extends PaymentMethod<?>, ?> account = model.getSortedAccounts().get(0);
            accountService.setSelectedAccount(account);
        }
    }

    private void applyDataDisplay(Account<? extends PaymentMethod<?>, ?> account) {
        if (account == null) {
            model.getAccountDetails().set(null);
            return;
        }

        AccountPayload<? extends PaymentMethod<?>> accountPayload = account.getAccountPayload();
        if (account.getPaymentMethod().getPaymentRail() instanceof FiatPaymentRail fiatPaymentRail) {
            AccountDetails<?, ?> accountDetails = getAccountDetails(account, fiatPaymentRail);
            model.getAccountDetails().set(accountDetails);
        }
    }

    private AccountDetails<?, ?> getAccountDetails(Account<? extends PaymentMethod<?>, ?> account,
                                                   FiatPaymentRail fiatPaymentRail) {
        return switch (fiatPaymentRail) {
            case CUSTOM -> new UserDefinedAccountDetails((UserDefinedFiatAccount) account);
            case SEPA -> new SepaAccountDetails((SepaAccount) account);
            case SEPA_INSTANT -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
            case ZELLE -> new ZelleAccountDetails((ZelleAccount) account);
            case REVOLUT -> new RevolutAccountDetails((RevolutAccount) account);
            case WISE -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
            case NATIONAL_BANK -> new NationalBankAccountDetails((NationalBankAccount) account);
            case SAME_BANK -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
            case SWIFT -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
            case F2F -> new F2FAccountDetails((F2FAccount) account);
            case WISE_USD -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
            case ACH_TRANSFER -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
            case PIX -> new PixAccountDetails((PixAccount) account);
            case HAL_CASH -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
            case PIN_4 -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
            case SWISH -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
            case FASTER_PAYMENTS -> new FasterPaymentsAccountDetails((FasterPaymentsAccount) account);
            case PAY_ID -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
            case US_POSTAL_MONEY_ORDER ->
                    throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
            case CASH_BY_MAIL -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
            case STRIKE -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
            case INTERAC_E_TRANSFER ->
                    throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
            case UPHOLD -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
            case AMAZON_GIFT_CARD ->
                    throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
            case CASH_DEPOSIT -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
            case PROMPT_PAY -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
            case UPI -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
            case BIZUM -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
            case CASH_APP -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
            case DOMESTIC_WIRE_TRANSFER ->
                    throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
            case MONEY_BEAM -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
            case MONEY_GRAM -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
        };
    }

    private void disposeUserDefinedAccountDetailsPin() {
        if (userDefinedAccountDetailsPin != null) {
            userDefinedAccountDetailsPin.unsubscribe();
            userDefinedAccountDetailsPin = null;
        }
    }
}