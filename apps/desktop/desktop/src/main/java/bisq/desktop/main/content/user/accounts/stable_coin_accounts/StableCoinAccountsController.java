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

package bisq.desktop.main.content.user.accounts.stable_coin_accounts;

import bisq.account.AccountService;
import bisq.account.accounts.Account;
import bisq.account.accounts.stable_coin.StableCoinAccount;
import bisq.account.payment_method.PaymentMethod;
import bisq.common.observable.Pin;
import bisq.common.observable.map.HashMapObserver;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.navigation.NavigationTarget;
import bisq.i18n.Res;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Comparator;
import java.util.Optional;

@Slf4j
public class StableCoinAccountsController implements Controller {
    private final StableCoinAccountsModel model;
    @Getter
    private final StableCoinAccountsView view;
    private final AccountService accountService;
    private Pin accountsPin, selectedAccountPin;
    private Subscription selectedAccountSubscription;

    public StableCoinAccountsController(ServiceProvider serviceProvider) {
        accountService = serviceProvider.getAccountService();

        model = new StableCoinAccountsModel();
        view = new StableCoinAccountsView(model, this);

        model.getSortedAccounts().setComparator(Comparator.comparing(Account::getAccountName));
    }

    @Override
    public void onActivate() {
        accountsPin = accountService.getAccountByNameMap().addObserver(new HashMapObserver<>() {
            @Override
            public void put(String key, Account<? extends PaymentMethod<?>, ?> account) {
                UIThread.run(() -> {
                    if (account instanceof StableCoinAccount stableCoinAccount &&
                            !model.getAccounts().contains(stableCoinAccount)) {
                        model.getAccounts().add(stableCoinAccount);
                        accountService.setSelectedAccount(account);
                        updateNoAccountsState();
                    }
                });
            }

            @Override
            public void remove(Object key) {
                if (key instanceof String accountName) {
                    UIThread.run(() -> {
                        findAccount(accountName).ifPresent(account -> model.getAccounts().remove(account));
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

        selectedAccountPin = accountService.selectedAccountAsObservable().addObserver(account ->
                UIThread.run(() -> {
                    if (account instanceof StableCoinAccount sc) {
                        model.getSelectedAccount().set(sc);
                    }
                }));

        selectedAccountSubscription = EasyBind.subscribe(model.getSelectedAccount(),
                selectedAccount -> {
                    if (selectedAccount != null) {
                        accountService.setSelectedAccount(selectedAccount);
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
        model.reset();
    }

    void onSelectAccount(StableCoinAccount account) {
        if (account != null) {
            accountService.setSelectedAccount(account);
            model.getSelectedAccount().set(account);
        }
    }

    void onCreateAccount() {
        Navigation.navigateTo(NavigationTarget.CREATE_STABLE_COIN_ACCOUNT);
    }

    void onDeleteAccount() {
        StableCoinAccount selected = model.getSelectedAccount().get();
        if (selected != null) {
            new Popup().warning(Res.get("user.stableCoinAccounts.deleteAccount.confirmQuestion"))
                    .actionButtonText(Res.get("user.stableCoinAccounts.deleteAccount"))
                    .onAction(() -> {
                        accountService.removePaymentAccount(selected);
                        maybeSelectFirstAccount();
                        updateNoAccountsState();
                    })
                    .closeButtonText(Res.get("action.cancel"))
                    .show();
        }
    }

    private void updateDeleteButtonStates() {
        model.getDeleteButtonDisabled().set(model.getSelectedAccount().get() == null);
    }

    private void updateNoAccountsState() {
        boolean hasNoAccounts = model.getAccounts().isEmpty();
        model.getNoAccountsAvailable().set(hasNoAccounts);
    }

    private void maybeSelectFirstAccount() {
        if (model.getSelectedAccount().get() == null && !model.getSortedAccounts().isEmpty()) {
            StableCoinAccount first = model.getSortedAccounts().get(0);
            accountService.setSelectedAccount(first);
            model.getSelectedAccount().set(first);
        }
    }

    private Optional<StableCoinAccount> findAccount(String accountName) {
        return model.getAccounts().stream()
                .filter(account -> account.getAccountName().equals(accountName))
                .findAny();
    }
}
