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

package bisq.desktop.main.content.user.accounts;

import bisq.account.AccountService;
import bisq.account.accounts.Account;
import bisq.account.accounts.UserDefinedFiatAccount;
import bisq.account.accounts.UserDefinedFiatAccountPayload;
import bisq.account.payment_method.PaymentMethod;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.i18n.Res;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Comparator;
import java.util.NoSuchElementException;

import static bisq.bisq_easy.NavigationTarget.CREATE_BISQ_EASY_PAYMENT_ACCOUNT;
import static com.google.common.base.Preconditions.*;

@Slf4j
public class PaymentAccountsController implements Controller {
    private final PaymentAccountsModel model;
    @Getter
    private final PaymentAccountsView view;
    private final AccountService accountService;
    private Subscription selectedAccountSubscription, accountDataSubscription;
    private Pin accountsPin, selectedAccountPin;


    public PaymentAccountsController(ServiceProvider serviceProvider) {
        accountService = serviceProvider.getAccountService();

        model = new PaymentAccountsModel();
        view = new PaymentAccountsView(model, this);
    }

    @Override
    public void onActivate() {
        model.getSortedAccounts().setComparator(Comparator.comparing(Account::getAccountName));

        accountsPin = accountService.getAccounts().addObserver(() -> UIThread.run(() -> {
            model.setAllAccounts(accountService.getAccounts());
            maybeSelectFirstAccount();
            model.getNoAccountsSetup().set(!accountService.hasAccounts());
            model.getHeadline().set(accountService.hasAccounts() ?
                    Res.get("user.paymentAccounts.headline") :
                    Res.get("user.paymentAccounts.noAccounts.headline")
            );
        }));

        selectedAccountPin = FxBindings.bind(model.selectedAccountProperty())
                .to(accountService.selectedAccountAsObservable());

        selectedAccountSubscription = EasyBind.subscribe(model.selectedAccountProperty(),
                selectedAccount -> {
                    if (selectedAccount instanceof UserDefinedFiatAccount) {
                        accountService.setSelectedAccount(selectedAccount);
                        model.setAccountData(((UserDefinedFiatAccount) selectedAccount).getAccountPayload().getAccountData());
                        updateButtonStates();
                    } else {
                        model.setAccountData("");
                    }
                });

        accountDataSubscription = EasyBind.subscribe(model.accountDataProperty(),
                accountData -> updateButtonStates());
    }

    @Override
    public void onDeactivate() {
        selectedAccountSubscription.unsubscribe();
        accountDataSubscription.unsubscribe();
        accountsPin.unbind();
        selectedAccountPin.unbind();
    }

    void onSelectAccount(Account<?, ? extends PaymentMethod<?>> account) {
        if (account != null) {
            accountService.setSelectedAccount(account);
        }
    }

    void onCreateAccount() {
        Navigation.navigateTo(CREATE_BISQ_EASY_PAYMENT_ACCOUNT);
    }

    void onSaveAccount() {
        var selectedAccount = this.getSelectedAccount();
        String accountName = selectedAccount.getAccountName();
        model.getAccountData().ifPresent(accountData -> {
            checkArgument(accountData.length() <= UserDefinedFiatAccountPayload.MAX_DATA_LENGTH, "Account data must not be longer than 1000 characters");
            UserDefinedFiatAccount newAccount = new UserDefinedFiatAccount(accountName, accountData);
            accountService.removePaymentAccount(selectedAccount);
            accountService.addPaymentAccount(newAccount);
            accountService.setSelectedAccount(newAccount);
        });
    }

    void onDeleteAccount() {
        accountService.removePaymentAccount(this.getSelectedAccount());
        maybeSelectFirstAccount();
    }

    private void updateButtonStates() {
        model.setSaveButtonDisabled(model.getSelectedAccount().isEmpty()
                || model.getAccountData().isEmpty()
                || ((UserDefinedFiatAccount) model.getSelectedAccount().get()).getAccountPayload().getAccountData()
                .equals(model.getAccountData().get()));

        model.setDeleteButtonDisabled(model.getSelectedAccount().isEmpty());
    }

    private void maybeSelectFirstAccount() {
        if (!model.getSortedAccounts().isEmpty() && accountService.getSelectedAccount().isEmpty()) {
            accountService.setSelectedAccount(model.getSortedAccounts().get(0));
        }
    }

    private Account<?, ? extends PaymentMethod<?>> getSelectedAccount() throws NoSuchElementException {
        return model.getSelectedAccount().orElseThrow(() -> new NoSuchElementException("There is no account selected."));
    }
}
