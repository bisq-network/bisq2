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

package bisq.desktop.main.content.user.accounts.crypto_accounts;

import bisq.account.AccountService;
import bisq.account.accounts.Account;
import bisq.account.accounts.crypto.CryptoAssetAccount;
import bisq.account.accounts.crypto.MoneroAccount;
import bisq.account.accounts.crypto.OtherCryptoAssetAccount;
import bisq.account.payment_method.PaymentMethod;
import bisq.account.timestamp.AccountTimestampService;
import bisq.common.file.FileReaderUtils;
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
import bisq.desktop.main.content.user.accounts.AccountDetails;
import bisq.desktop.main.content.user.accounts.crypto_accounts.details.CryptoAccountDetails;
import bisq.desktop.main.content.user.accounts.crypto_accounts.details.MoneroAccountDetails;
import bisq.desktop.main.content.user.accounts.crypto_accounts.details.OtherCryptoAssetAccountDetails;
import bisq.desktop.navigation.NavigationTarget;
import bisq.mu_sig.MuSigService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Comparator;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class CryptoAssetAccountsController implements Controller {
    private final CryptoAssetAccountsModel model;
    @Getter
    private final CryptoAssetAccountsView view;
    private final AccountService accountService;
    private final MuSigService muSigService;
    private final AccountTimestampService accountTimestampService;
    private Pin selectedAccountPin, accountsPin;
    private Subscription selectedAccountSubscription;

    public CryptoAssetAccountsController(ServiceProvider serviceProvider) {
        accountService = serviceProvider.getAccountService();
        muSigService = serviceProvider.getMuSigService();
        accountTimestampService = accountService.getAccountTimestampService();

        model = new CryptoAssetAccountsModel();
        view = new CryptoAssetAccountsView(model, this);

        model.getSortedAccounts().setComparator(Comparator.comparing(Account::getAccountName));
    }

    @Override
    public void onActivate() {
        model.getImportBisq1AccountDataButtonVisible().setValue(muSigService.getMuSigActivated().get());

        accountsPin = accountService.getAccountByNameMap().addObserver(new HashMapObserver<>() {
            @Override
            public void put(String key, Account<? extends PaymentMethod<?>, ?> account) {
                UIThread.run(() -> {
                    if (account instanceof CryptoAssetAccount &&
                            !model.getAccounts().contains(account)) {
                        model.getAccounts().add(account);
                        accountService.setSelectedAccount(account);
                        updateNoAccountsState();
                    }
                });
            }

            @Override
            public void remove(Object key) {
                if (key instanceof String accountName) {
                    UIThread.run(() -> {
                        findAccount(accountName)
                                .ifPresent(account -> model.getAccounts().remove(account));
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
                    if (selectedAccount instanceof CryptoAssetAccount<?>) {
                        accountService.setSelectedAccount(selectedAccount);
                        applyDataDisplay(selectedAccount);
                        updateDeleteButtonStates();
                    }
                });

        maybeSelectFirstAccount();
        updateNoAccountsState();
    }

    @Override
    public void onDeactivate() {
        accountsPin.unbind();
        selectedAccountPin.unbind();
        selectedAccountSubscription.unsubscribe();
        AccountDetails<?, ?> accountDetails = model.getAccountDetails().get();
        if (accountDetails != null) {
            accountDetails.dispose();
        }
        model.reset();
    }

    void onSelectAccount(Account<? extends PaymentMethod<?>, ?> account) {
        if (account != null) {
            accountService.setSelectedAccount(account);
            model.getSelectedAccount().set(account);
        }
    }

    void onCreateAccount() {
        if (muSigService.getMuSigActivated().get()) {
            Navigation.navigateTo(NavigationTarget.CREATE_CRYPTO_CURRENCY_ACCOUNT);
        }
    }

    void onDeleteAccount() {
        accountService.removePaymentAccount(model.getSelectedAccount().get());

        maybeSelectFirstAccount();
        updateNoAccountsState();
    }

    void onImportBisq1AccountData() {
        FileChooserUtil.openFile(getView().getRoot().getScene())
                .ifPresent(path -> {
                    try {
                        String json = FileReaderUtils.readUTF8String(path);
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
        boolean hasNoAccounts = accountService.getCryptoAssetAccounts().isEmpty();
        model.getNoAccountsAvailable().set(hasNoAccounts);
        if (hasNoAccounts) {
            model.getAccountDetails().set(null);
        }
    }

    private void maybeSelectFirstAccount() {
        if (accountService.findSelectedAccount().isEmpty() && !model.getSortedAccounts().isEmpty()) {
            Account<? extends PaymentMethod<?>, ?> account = model.getSortedAccounts().get(0);
            accountService.setSelectedAccount(account);
        }
    }

    private void applyDataDisplay(Account<? extends PaymentMethod<?>, ?> account) {
        if (account == null) {
            model.getAccountDetails().set(null);
            return;
        }

        CryptoAccountDetails<?> accountDetails = getAccountDetails(account);
        model.getAccountDetails().set(accountDetails);
    }

    private CryptoAccountDetails<?> getAccountDetails(Account<?, ?> cryptoAssetAccount) {
        if (cryptoAssetAccount instanceof MoneroAccount moneroAccount) {
            return new MoneroAccountDetails(moneroAccount, accountTimestampService);
        } else if (cryptoAssetAccount instanceof OtherCryptoAssetAccount otherCryptoAssetAccount) {
            return new OtherCryptoAssetAccountDetails(otherCryptoAssetAccount, accountTimestampService);
        } else {
            throw new UnsupportedOperationException("Unsupported cryptoAssetAccount " + cryptoAssetAccount.getClass().getSimpleName());
        }
    }

    private Optional<Account<? extends PaymentMethod<?>, ?>> findAccount(String key) {
        return model.getAccounts().stream()
                .filter(account -> account.getAccountName().equals(key))
                .findAny();
    }
}
