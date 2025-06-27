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
import bisq.account.accounts.AccountPayload;
import bisq.account.accounts.F2FAccount;
import bisq.account.accounts.SepaAccount;
import bisq.account.accounts.UserDefinedFiatAccount;
import bisq.account.payment_method.FiatPaymentRail;
import bisq.account.payment_method.PaymentMethod;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.main.content.user.accounts.details.AccountDetailsVBox;
import bisq.desktop.main.content.user.accounts.details.F2FAccountDetailsVBox;
import bisq.desktop.main.content.user.accounts.details.SepaAccountDetailsVBox;
import bisq.desktop.main.content.user.accounts.details.UserDefinedAccountDetailsVBox;
import bisq.desktop.navigation.NavigationTarget;
import bisq.i18n.Res;
import bisq.mu_sig.MuSigService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Comparator;

@Slf4j
public class PaymentAccountsController implements Controller {
    private final PaymentAccountsModel model;
    @Getter
    private final PaymentAccountsView view;
    private final AccountService accountService;
    private final MuSigService muSigService;
    private Subscription selectedAccountSubscription;
    private Pin accountsPin, selectedAccountPin;

    public PaymentAccountsController(ServiceProvider serviceProvider) {
        accountService = serviceProvider.getAccountService();
        muSigService = serviceProvider.getMuSigService();

        model = new PaymentAccountsModel();
        view = new PaymentAccountsView(model, this);

        model.getSortedAccounts().setComparator(Comparator.comparing(Account::getAccountName));
    }

    @Override
    public void onActivate() {
        accountsPin = accountService.getAccounts().addObserver(new CollectionObserver<>() {
            @Override
            public void add(Account<? extends PaymentMethod<?>, ?> account) {
                UIThread.run(() -> {
                    if (!model.getAccounts().contains(account)) {
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
                    updateButtonStates();
                });

        maybeSelectFirstAccount();
        updateNoAccountsState();
    }

    private void updateNoAccountsState() {
        model.getNoAccountsSetup().set(!accountService.hasAccounts());
        model.getHeadline().set(accountService.hasAccounts() ?
                Res.get("user.paymentAccounts.headline") :
                Res.get("user.paymentAccounts.noAccounts.headline")
        );
    }

    @Override
    public void onDeactivate() {
        accountsPin.unbind();
        selectedAccountPin.unbind();
        selectedAccountSubscription.unsubscribe();
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

    void onDeleteAccount() {
        Account<? extends PaymentMethod<?>, ?> account = model.getSelectedAccount().get();
        accountService.removePaymentAccount(account);
        model.getAccounts().remove(account);
        maybeSelectFirstAccount();
    }

    private void updateButtonStates() {
        model.getDeleteButtonDisabled().set(model.getSelectedAccount().get() == null);
    }

    private void maybeSelectFirstAccount() {
        if (accountService.getSelectedAccount().isEmpty() && !model.getSortedAccounts().isEmpty()) {
            Account<? extends PaymentMethod<?>, ?> account = model.getSortedAccounts().get(0);
            accountService.setSelectedAccount(account);
        }
    }

    private void applyDataDisplay(Account<? extends PaymentMethod<?>, ?> account) {
        if (account == null) {
            model.getAccountDetailsGridPane().set(null);
            return;
        }

        AccountPayload<? extends PaymentMethod<?>> accountPayload = account.getAccountPayload();
        if (account.getPaymentMethod().getPaymentRail() instanceof FiatPaymentRail fiatPaymentRail) {
            AccountDetailsVBox<?,?> detailsVBox = getAccountDetailsVBox(account, fiatPaymentRail);
            model.getAccountDetailsGridPane().set(detailsVBox);
        }
    }

    private AccountDetailsVBox<?, ?> getAccountDetailsVBox(Account<? extends PaymentMethod<?>, ?> account,
                                                           FiatPaymentRail fiatPaymentRail) {
        return switch (fiatPaymentRail) {
            case CUSTOM -> new UserDefinedAccountDetailsVBox((UserDefinedFiatAccount) account);
            case SEPA -> new SepaAccountDetailsVBox((SepaAccount) account);
            case SEPA_INSTANT -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
            case ZELLE -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
            case REVOLUT -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
            case WISE -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
            case NATIONAL_BANK -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
            case SWIFT -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
            case F2F -> new F2FAccountDetailsVBox((F2FAccount) account);
            case ACH_TRANSFER -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
            case PIX -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
            case FASTER_PAYMENTS -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
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
        };
    }
}