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

package bisq.desktop.main.content.user.accounts.fiat_accounts.create.data;

import bisq.account.accounts.AccountPayload;
import bisq.account.payment_method.PaymentMethod;
import bisq.account.payment_method.PaymentRail;
import bisq.account.payment_method.crypto.CryptoPaymentRail;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.data.form.AchTransferFormController;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.data.form.AdvancedCashFormController;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.data.form.AliPayFormController;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.data.form.AmazonGiftCardFormController;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.data.form.BizumFormController;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.data.form.CashByMailFormController;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.data.form.CashDepositFormController;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.data.form.DomesticWireTransferFormController;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.data.form.F2FFormController;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.data.form.FasterPaymentsFormController;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.data.form.FormController;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.data.form.HalCashFormController;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.data.form.ImpsFormController;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.data.form.InteracETransferFormController;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.data.form.MercadoPagoFormController;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.data.form.MoneseFormController;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.data.form.MoneyBeamFormController;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.data.form.MoneyGramFormController;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.data.form.NationalBankFormController;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.data.form.NeftFormController;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.data.form.PayIdFormController;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.data.form.PayseraFormController;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.data.form.PerfectMoneyFormController;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.data.form.Pin4FormController;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.data.form.PixFormController;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.data.form.PromptPayFormController;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.data.form.RevolutFormController;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.data.form.SameBankFormController;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.data.form.SatispayFormController;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.data.form.SbpFormController;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.data.form.SepaFormController;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.data.form.SepaInstantFormController;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.data.form.StrikeFormController;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.data.form.SwiftFormController;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.data.form.SwishFormController;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.data.form.USPostalMoneyOrderFormController;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.data.form.UpholdFormController;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.data.form.UpiFormController;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.data.form.WeChatPayFormController;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.data.form.WiseFormController;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.data.form.WiseUsdFormController;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.data.form.ZelleFormController;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.scene.layout.StackPane;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class AccountDataController implements Controller {
    private final AccountDataModel model;
    @Getter
    private final AccountDataView view;
    private final ServiceProvider serviceProvider;
    @Nullable
    private FormController<?, ?, ?> paymentFormController;

    public AccountDataController(ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;

        model = new AccountDataModel();
        view = new AccountDataView(model, this);
    }

    @Nullable
    public ReadOnlyBooleanProperty getShowOverlay() {
        return paymentFormController != null ? paymentFormController.getShowOverlay() : null;
    }

    @Nullable
    public AccountPayload<?> getAccountPayload() {
        return paymentFormController != null ? paymentFormController.createAccountPayload() : null;
    }

    public void setPaymentMethod(PaymentMethod<?> paymentMethod) {
        checkNotNull(paymentMethod, "PaymentMethod must not be null");
        model.setPaymentMethod(paymentMethod);

        applyPaymentMethod(paymentMethod);
    }

    @Override
    public void onActivate() {
        applyPaymentMethod(model.getPaymentMethod());
    }

    @Override
    public void onDeactivate() {
        model.setPaymentForm(null);
    }

    public boolean validate() {
        return paymentFormController != null && paymentFormController.validate();
    }

    private FormController<?, ?, ?> getOrCreateController(PaymentMethod<?> paymentMethod) {
        String key = paymentMethod.getPaymentRail().name();
        return model.getControllerCache().computeIfAbsent(key, k -> createController(paymentMethod));
    }

    private FormController<?, ?, ?> createController(PaymentMethod<?> paymentMethod) {
        PaymentRail paymentRail = paymentMethod.getPaymentRail();
        if (paymentRail instanceof FiatPaymentRail fiatPaymentRail) {
            return getPaymentFormController(fiatPaymentRail);
        } else if (paymentRail instanceof CryptoPaymentRail cryptoPaymentRail) {
            throw new UnsupportedOperationException("CryptoPaymentRail not implemented yet");
        } else {
            throw new UnsupportedOperationException("No implementation found for " + paymentRail.name());
        }
    }

    private FormController<?, ?, ?> getPaymentFormController(PaymentRail paymentRail) {
        if (paymentRail instanceof FiatPaymentRail fiatPaymentRail) {
            return getFiatPaymentFormController(fiatPaymentRail);
        } else {
            throw new UnsupportedOperationException("PaymentRail not supported: " + paymentRail.name());
        }
    }

    private FormController<?, ?, ?> getFiatPaymentFormController(FiatPaymentRail fiatPaymentRail) {
        return switch (fiatPaymentRail) {
            case ACH_TRANSFER -> new AchTransferFormController(serviceProvider);
            case ADVANCED_CASH -> new AdvancedCashFormController(serviceProvider);
            case ALI_PAY -> new AliPayFormController(serviceProvider);
            case AMAZON_GIFT_CARD -> new AmazonGiftCardFormController(serviceProvider);
            case BIZUM -> new BizumFormController(serviceProvider);
            case CASH_APP -> throw new UnsupportedOperationException("Not implemented yet");
            case CASH_BY_MAIL -> new CashByMailFormController(serviceProvider);
            case CASH_DEPOSIT -> new CashDepositFormController(serviceProvider);
            case CUSTOM -> throw new UnsupportedOperationException("Not implemented yet");
            case DOMESTIC_WIRE_TRANSFER -> new DomesticWireTransferFormController(serviceProvider);
            case F2F -> new F2FFormController(serviceProvider);
            case FASTER_PAYMENTS -> new FasterPaymentsFormController(serviceProvider);
            case HAL_CASH -> new HalCashFormController(serviceProvider);
            case IMPS -> new ImpsFormController(serviceProvider);
            case INTERAC_E_TRANSFER -> new InteracETransferFormController(serviceProvider);
            case MERCADO_PAGO -> new MercadoPagoFormController(serviceProvider);
            case MONESE -> new MoneseFormController(serviceProvider);
            case MONEY_BEAM -> new MoneyBeamFormController(serviceProvider);
            case MONEY_GRAM -> new MoneyGramFormController(serviceProvider);
            case NATIONAL_BANK -> new NationalBankFormController(serviceProvider);
            case NEFT -> new NeftFormController(serviceProvider);
            case PAY_ID -> new PayIdFormController(serviceProvider);
            case PAYSERA -> new PayseraFormController(serviceProvider);
            case PERFECT_MONEY -> new PerfectMoneyFormController(serviceProvider);
            case PIN_4 -> new Pin4FormController(serviceProvider);
            case PIX -> new PixFormController(serviceProvider);
            case PROMPT_PAY -> new PromptPayFormController(serviceProvider);
            case REVOLUT -> new RevolutFormController(serviceProvider);
            case SAME_BANK -> new SameBankFormController(serviceProvider);
            case SATISPAY -> new SatispayFormController(serviceProvider);
            case SBP -> new SbpFormController(serviceProvider);
            case SEPA -> new SepaFormController(serviceProvider);
            case SEPA_INSTANT -> new SepaInstantFormController(serviceProvider);
            case STRIKE -> new StrikeFormController(serviceProvider);
            case SWIFT -> new SwiftFormController(serviceProvider);
            case SWISH -> new SwishFormController(serviceProvider);
            case UPHOLD -> new UpholdFormController(serviceProvider);
            case UPI -> new UpiFormController(serviceProvider);
            case US_POSTAL_MONEY_ORDER -> new USPostalMoneyOrderFormController(serviceProvider);
            case WECHAT_PAY -> new WeChatPayFormController(serviceProvider);
            case WISE -> new WiseFormController(serviceProvider);
            case WISE_USD -> new WiseUsdFormController(serviceProvider);
            case ZELLE -> new ZelleFormController(serviceProvider);
        };
    }

    private void applyPaymentMethod(PaymentMethod<?> paymentMethod) {
        if (paymentMethod != null) {
            paymentFormController = getOrCreateController(paymentMethod);
            StackPane root = paymentFormController.getView().getRoot();
            model.setPaymentForm(root);
        }
    }
}
