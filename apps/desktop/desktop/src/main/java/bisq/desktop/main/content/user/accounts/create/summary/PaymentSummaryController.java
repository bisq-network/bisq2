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
import bisq.account.accounts.AccountPayload;
import bisq.account.accounts.F2FAccount;
import bisq.account.accounts.F2FAccountPayload;
import bisq.account.accounts.SepaAccountPayload;
import bisq.account.payment_method.FiatPaymentRail;
import bisq.account.payment_method.PaymentMethod;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.user.accounts.create.summary.data_display.F2FDataDisplay;
import bisq.desktop.main.content.user.accounts.create.summary.data_display.SepaDataDisplay;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class PaymentSummaryController implements Controller {
    private final PaymentSummaryModel model;
    @Getter
    private PaymentSummaryView view;
    private final AccountService accountService;
    private final Runnable createAccountHandler;

    public PaymentSummaryController(ServiceProvider serviceProvider, Runnable createAccountHandler) {
        accountService = serviceProvider.getAccountService();
        this.createAccountHandler = createAccountHandler;
        model = new PaymentSummaryModel();
        view = new PaymentSummaryView(model, this);
    }


    public void setPaymentMethod(PaymentMethod<?> paymentMethod) {
        checkNotNull(paymentMethod, "PaymentMethod must not be null");
        model.setPaymentMethod(paymentMethod);
    }

    public void setAccountPayload(AccountPayload accountPayload) {
        model.setAccountPayload(accountPayload);

        if (model.getPaymentMethod().getPaymentRail() instanceof FiatPaymentRail fiatPaymentRail) {
            model.setRisk(fiatPaymentRail.getChargebackRisk().getDisplayString());
            model.setTradeLimit("TODO limit for " + fiatPaymentRail.getChargebackRisk().getDisplayString());

            switch (fiatPaymentRail) {
                case CUSTOM -> {
                }
                case SEPA -> {model.setDataDisplay(new SepaDataDisplay((SepaAccountPayload) accountPayload));
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
                    model.setDataDisplay(new F2FDataDisplay((F2FAccountPayload) accountPayload));
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

    public void createAccount() {
        if (model.getPaymentMethod().getPaymentRail() instanceof FiatPaymentRail fiatPaymentRail) {
            switch (fiatPaymentRail) {
                case CUSTOM -> {
                }
                case SEPA -> {
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
                    F2FAccountPayload f2FAccountPayload = (F2FAccountPayload) model.getAccountPayload();
                    accountService.addPaymentAccount(new F2FAccount(f2FAccountPayload));
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

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }
}