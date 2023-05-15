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

package bisq.desktop.primary.main.content.settings.accounts;

import bisq.account.bisqeasy.BisqEasyPaymentAccount;
import bisq.account.bisqeasy.BisqEasyPaymentAccountService;
import bisq.application.DefaultApplicationService;
import bisq.common.observable.Pin;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Comparator;

import static bisq.desktop.common.view.NavigationTarget.CREATE_BISQ_EASY_PAYMENT_ACCOUNT;
import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class PaymentAccountsController implements Controller {
    private final PaymentAccountsModel model;
    @Getter
    private final PaymentAccountsView view;
    private final BisqEasyPaymentAccountService service;
    private Subscription selectedAccountSubscription, accountDataSubscription;
    private Pin accountsPin, selectedAccountPin, selectedAccountPin2;


    public PaymentAccountsController(DefaultApplicationService applicationService) {
        service = applicationService.getAccountService().getBisqEasyPaymentAccountService();

        model = new PaymentAccountsModel();
        view = new PaymentAccountsView(model, this);
    }

    @Override
    public void onActivate() {
        model.getSortedAccounts().setComparator(Comparator.comparing(BisqEasyPaymentAccount::getName));

        accountsPin = service.getAccounts().addListener(() -> {
            model.setAllAccounts(service.getAccounts());
            maybeSelectFirstAccount();
        });
        selectedAccountPin = FxBindings.bind(model.selectedAccountProperty())
                .to(service.selectedAccountAsObservable());

        selectedAccountSubscription = EasyBind.subscribe(model.selectedAccountProperty(),
                selectedAccount -> {
                    if (selectedAccount != null) {
                        service.setSelectedAccount(selectedAccount);
                        model.setAccountData(selectedAccount.getData());
                        updateButtonStates();
                    } else {
                        model.setAccountData("");
                    }
                });

        accountDataSubscription = EasyBind.subscribe(model.accountDataProperty(),
                accountData -> updateButtonStates());

        if (!service.hasAccounts()) {
            onCreateAccount();
        }
    }

    @Override
    public void onDeactivate() {
        selectedAccountSubscription.unsubscribe();
        accountDataSubscription.unsubscribe();
        accountsPin.unbind();
        selectedAccountPin.unbind();
    }

    void onSelectAccount(BisqEasyPaymentAccount account) {
        if (account != null) {
            service.setSelectedAccount(account);
        }
    }

    void onCreateAccount() {
        Navigation.navigateTo(CREATE_BISQ_EASY_PAYMENT_ACCOUNT);
    }

    void onSaveAccount() {
        checkNotNull(model.getSelectedAccount());
        String accountName = model.getSelectedAccount().getName();
        String accountData = model.getAccountData();
        BisqEasyPaymentAccount newAccount = new BisqEasyPaymentAccount(accountName, accountData);

        service.removePaymentAccount(model.getSelectedAccount());
        service.addPaymentAccount(newAccount);
        service.setSelectedAccount(newAccount);
    }

    void onDeleteAccount() {
        checkNotNull(model.getSelectedAccount());
        service.removePaymentAccount(model.getSelectedAccount());
        maybeSelectFirstAccount();
    }

    private void updateButtonStates() {
        model.setSaveButtonDisabled(model.getSelectedAccount() == null
                || model.getAccountData() == null
                || model.getAccountData().isEmpty()
                || model.getSelectedAccount().getData().equals(model.getAccountData()));

        model.setDeleteButtonDisabled(model.getSelectedAccount() == null);
    }

    private void maybeSelectFirstAccount() {
        if (!model.getSortedAccounts().isEmpty() && service.getSelectedAccount() == null) {
            service.setSelectedAccount(model.getSortedAccounts().get(0));
        }
    }
}
