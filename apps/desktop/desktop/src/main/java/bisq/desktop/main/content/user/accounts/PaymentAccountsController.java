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
import bisq.account.accounts.F2FAccountPayload;
import bisq.account.accounts.SepaAccountPayload;
import bisq.account.accounts.UserDefinedFiatAccount;
import bisq.account.accounts.UserDefinedFiatAccountPayload;
import bisq.account.payment_method.FiatPaymentRail;
import bisq.account.payment_method.PaymentMethod;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
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
import java.util.Optional;

@Slf4j
public class PaymentAccountsController implements Controller {
    private final PaymentAccountsModel model;
    @Getter
    private final PaymentAccountsView view;
    private final AccountService accountService;
    private final MuSigService muSigService;
    private Subscription selectedAccountSubscription;
    private Pin selectedAccountPin;

    public PaymentAccountsController(ServiceProvider serviceProvider) {
        accountService = serviceProvider.getAccountService();
        muSigService = serviceProvider.getMuSigService();

        model = new PaymentAccountsModel();
        view = new PaymentAccountsView(model, this);
    }

    @Override
    public void onActivate() {
        model.getSortedAccounts().setComparator(Comparator.comparing(Account::getAccountName));

        model.getAccounts().setAll(accountService.getAccounts());
        maybeSelectFirstAccount();
        model.getNoAccountsSetup().set(!accountService.hasAccounts());
        model.getHeadline().set(accountService.hasAccounts() ?
                Res.get("user.paymentAccounts.headline") :
                Res.get("user.paymentAccounts.noAccounts.headline")
        );

        selectedAccountPin = FxBindings.bind(model.getSelectedAccount())
                .to(accountService.selectedAccountAsObservable());

        selectedAccountSubscription = EasyBind.subscribe(model.getSelectedAccount(),
                selectedAccount -> {
                    accountService.setSelectedAccount(selectedAccount);
                    applyDataDisplay(selectedAccount);
                    updateButtonStates();
                });
    }

    @Override
    public void onDeactivate() {
        selectedAccountPin.unbind();
        selectedAccountSubscription.unsubscribe();
    }

    void onSelectAccount(Account<?, ? extends PaymentMethod<?>> account) {
        if (account != null) {
            accountService.setSelectedAccount(account);
        }
    }

    private void applyDataDisplay(Account<?, ? extends PaymentMethod<?>> account) {
        AccountPayload accountPayload = account.getAccountPayload();
        model.setAccountPayload(accountPayload);
        if (account.getPaymentMethod().getPaymentRail() instanceof FiatPaymentRail fiatPaymentRail) {

            switch (fiatPaymentRail) {
                case CUSTOM -> {
                    model.getAccountDetailsGridPane().set(new UserDefinedAccountDetailsVBox((UserDefinedFiatAccountPayload) accountPayload));
                }
                case SEPA -> {
                    model.getAccountDetailsGridPane().set(new SepaAccountDetailsVBox((SepaAccountPayload) accountPayload));
                }
                case SEPA_INSTANT -> {
                }
                case ZELLE -> {
                }
                case REVOLUT -> {
                }
                case WISE -> {
                }
                case NATIONAL_BANK -> {
                }
                case SWIFT -> {
                }
                case F2F -> {
                    model.getAccountDetailsGridPane().set(new F2FAccountDetailsVBox((F2FAccountPayload) accountPayload));
                }
                case ACH_TRANSFER -> {
                }
                case PIX -> {
                }
                case FASTER_PAYMENTS -> {
                }
                case PAY_ID -> {
                }
                case US_POSTAL_MONEY_ORDER -> {
                }
                case CASH_BY_MAIL -> {
                }
                case STRIKE -> {
                }
                case INTERAC_E_TRANSFER -> {
                }
                case AMAZON_GIFT_CARD -> {
                }
                case CASH_DEPOSIT -> {
                }
                case UPI -> {
                }
                case BIZUM -> {
                }
                case CASH_APP -> {
                }
            }
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
        accountService.removePaymentAccount(model.getSelectedAccount().get());
        maybeSelectFirstAccount();
    }

    private void updateButtonStates() {
        //todo
        if (Optional.ofNullable(model.getSelectedAccount().get()).isPresent() &&
                model.getSelectedAccount().get() instanceof UserDefinedFiatAccount) {
            model.getDeleteButtonDisabled().set(model.getSelectedAccount().get() == null);
        }
    }

    private void maybeSelectFirstAccount() {
        if (!model.getSortedAccounts().isEmpty()) {
            accountService.setSelectedAccount(model.getSortedAccounts().get(0));
        }
    }
}