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

package bisq.desktop.main.content.user.crypto_accounts;

import bisq.account.AccountService;
import bisq.account.accounts.Account;
import bisq.account.accounts.crypto.CryptoCurrencyAccount;
import bisq.account.accounts.crypto.MoneroAccount;
import bisq.account.accounts.crypto.OtherCryptoCurrencyAccount;
import bisq.account.payment_method.PaymentMethod;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.main.content.user.crypto_accounts.details.AccountDetails;
import bisq.desktop.main.content.user.crypto_accounts.details.MoneroAccountDetails;
import bisq.desktop.main.content.user.crypto_accounts.details.OtherCryptoCurrencyAccountDetails;
import bisq.desktop.navigation.NavigationTarget;
import bisq.mu_sig.MuSigService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Comparator;

@Slf4j
public class CryptoCurrencyAccountsController implements Controller {
    private final CryptoCurrencyAccountsModel model;
    @Getter
    private final CryptoCurrencyAccountsView view;
    private final AccountService accountService;
    private final MuSigService muSigService;
    private Subscription selectedAccountPin;
    private Pin accountsPin;

    public CryptoCurrencyAccountsController(ServiceProvider serviceProvider) {
        accountService = serviceProvider.getAccountService();
        muSigService = serviceProvider.getMuSigService();

        model = new CryptoCurrencyAccountsModel();
        view = new CryptoCurrencyAccountsView(model, this);

        model.getSortedAccounts().setComparator(Comparator.comparing(Account::getAccountName));
    }

    @Override
    public void onActivate() {
        accountsPin = accountService.getAccounts().addObserver(new CollectionObserver<>() {
            @Override
            public void add(Account<? extends PaymentMethod<?>, ?> account) {
                UIThread.run(() -> {
                    if (account instanceof CryptoCurrencyAccount<?> cryptoCurrencyAccount &&
                            !model.getAccounts().contains(cryptoCurrencyAccount)) {
                        model.getAccounts().add(cryptoCurrencyAccount);
                        handleAccountChange();
                    }
                });
            }

            @Override
            public void remove(Object element) {
                if (element instanceof CryptoCurrencyAccount<?> cryptoCurrencyAccount) {
                    UIThread.run(() -> {
                        model.getAccounts().remove(cryptoCurrencyAccount);
                        handleAccountChange();
                    });
                }
            }

            @Override
            public void clear() {
                UIThread.run(() -> {
                    model.getAccounts().clear();
                    handleAccountChange();
                });
            }
        });

        selectedAccountPin = EasyBind.subscribe(model.getSelectedAccount(),
                selectedAccount -> {
                    applyDataDisplay(selectedAccount);
                    updateButtonStates();
                });

        handleAccountChange();
    }

    @Override
    public void onDeactivate() {
        accountsPin.unbind();
        selectedAccountPin.unsubscribe();
        model.reset();
    }

    void onSelectAccount(CryptoCurrencyAccount<?> cryptoCurrencyAccount) {
        if (cryptoCurrencyAccount != null) {
            model.getSelectedAccount().set(cryptoCurrencyAccount);
        }
    }

    void onCreateAccount() {
        if (muSigService.getMuSigActivated().get()) {
            Navigation.navigateTo(NavigationTarget.CREATE_CRYPTO_CURRENCY_ACCOUNT);
        }
    }

    void onDeleteAccount() {
        CryptoCurrencyAccount<?> cryptoCurrencyAccount = model.getSelectedAccount().get();
        accountService.removePaymentAccount(cryptoCurrencyAccount);
        model.getAccounts().remove(cryptoCurrencyAccount);
    }

    private void handleAccountChange() {
        boolean hasAccounts = !model.getAccounts().isEmpty();
        model.getSelectedAccount().set(hasAccounts ? model.getSortedAccounts().getFirst() : null);
        model.getNoAccountsAvailable().set(!hasAccounts);
    }

    private void applyDataDisplay(CryptoCurrencyAccount<?> cryptoCurrencyAccount) {
        if (cryptoCurrencyAccount == null) {
            model.getAccountDetails().set(null);
        } else {
            model.getAccountDetails().set(getAccountDetails(cryptoCurrencyAccount));
        }
    }

    private void updateButtonStates() {
        model.getDeleteButtonDisabled().set(model.getSelectedAccount().get() == null);
    }

    private AccountDetails<?> getAccountDetails(CryptoCurrencyAccount<?> cryptoCurrencyAccount) {
        if (cryptoCurrencyAccount instanceof MoneroAccount moneroAccount) {
            return new MoneroAccountDetails(moneroAccount);
        } else if (cryptoCurrencyAccount instanceof OtherCryptoCurrencyAccount otherCryptoCurrencyAccount) {
            return new OtherCryptoCurrencyAccountDetails(otherCryptoCurrencyAccount);
        } else {
            throw new UnsupportedOperationException("Unsupported cryptoCurrencyAccount " + cryptoCurrencyAccount.getClass().getSimpleName());
        }
    }
}