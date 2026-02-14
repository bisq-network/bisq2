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

package bisq.desktop.main.content.user.accounts.fiat_accounts.create.summary;

import bisq.account.AccountService;
import bisq.account.accounts.Account;
import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.AccountPayload;
import bisq.account.accounts.MultiCurrencyAccountPayload;
import bisq.account.accounts.SelectableCurrencyAccountPayload;
import bisq.account.accounts.SingleCurrencyAccountPayload;
import bisq.account.accounts.fiat.AchTransferAccount;
import bisq.account.accounts.fiat.AchTransferAccountPayload;
import bisq.account.accounts.fiat.AdvancedCashAccount;
import bisq.account.accounts.fiat.AdvancedCashAccountPayload;
import bisq.account.accounts.fiat.AliPayAccount;
import bisq.account.accounts.fiat.AliPayAccountPayload;
import bisq.account.accounts.fiat.AmazonGiftCardAccount;
import bisq.account.accounts.fiat.AmazonGiftCardAccountPayload;
import bisq.account.accounts.fiat.BizumAccount;
import bisq.account.accounts.fiat.BizumAccountPayload;
import bisq.account.accounts.fiat.CashByMailAccount;
import bisq.account.accounts.fiat.CashByMailAccountPayload;
import bisq.account.accounts.fiat.CashDepositAccount;
import bisq.account.accounts.fiat.CashDepositAccountPayload;
import bisq.account.accounts.fiat.CountryBasedAccountPayload;
import bisq.account.accounts.fiat.DomesticWireTransferAccount;
import bisq.account.accounts.fiat.DomesticWireTransferAccountPayload;
import bisq.account.accounts.fiat.F2FAccount;
import bisq.account.accounts.fiat.F2FAccountPayload;
import bisq.account.accounts.fiat.FasterPaymentsAccount;
import bisq.account.accounts.fiat.FasterPaymentsAccountPayload;
import bisq.account.accounts.fiat.HalCashAccount;
import bisq.account.accounts.fiat.HalCashAccountPayload;
import bisq.account.accounts.fiat.ImpsAccount;
import bisq.account.accounts.fiat.ImpsAccountPayload;
import bisq.account.accounts.fiat.InteracETransferAccount;
import bisq.account.accounts.fiat.InteracETransferAccountPayload;
import bisq.account.accounts.fiat.MercadoPagoAccount;
import bisq.account.accounts.fiat.MercadoPagoAccountPayload;
import bisq.account.accounts.fiat.MoneseAccount;
import bisq.account.accounts.fiat.MoneseAccountPayload;
import bisq.account.accounts.fiat.MoneyBeamAccount;
import bisq.account.accounts.fiat.MoneyBeamAccountPayload;
import bisq.account.accounts.fiat.MoneyGramAccount;
import bisq.account.accounts.fiat.MoneyGramAccountPayload;
import bisq.account.accounts.fiat.NationalBankAccount;
import bisq.account.accounts.fiat.NationalBankAccountPayload;
import bisq.account.accounts.fiat.NeftAccount;
import bisq.account.accounts.fiat.NeftAccountPayload;
import bisq.account.accounts.fiat.PayIdAccount;
import bisq.account.accounts.fiat.PayIdAccountPayload;
import bisq.account.accounts.fiat.PayseraAccount;
import bisq.account.accounts.fiat.PayseraAccountPayload;
import bisq.account.accounts.fiat.PerfectMoneyAccount;
import bisq.account.accounts.fiat.PerfectMoneyAccountPayload;
import bisq.account.accounts.fiat.Pin4Account;
import bisq.account.accounts.fiat.Pin4AccountPayload;
import bisq.account.accounts.fiat.PixAccount;
import bisq.account.accounts.fiat.PixAccountPayload;
import bisq.account.accounts.fiat.PromptPayAccount;
import bisq.account.accounts.fiat.PromptPayAccountPayload;
import bisq.account.accounts.fiat.RevolutAccount;
import bisq.account.accounts.fiat.RevolutAccountPayload;
import bisq.account.accounts.fiat.SameBankAccount;
import bisq.account.accounts.fiat.SameBankAccountPayload;
import bisq.account.accounts.fiat.SatispayAccount;
import bisq.account.accounts.fiat.SatispayAccountPayload;
import bisq.account.accounts.fiat.SbpAccount;
import bisq.account.accounts.fiat.SbpAccountPayload;
import bisq.account.accounts.fiat.SepaAccount;
import bisq.account.accounts.fiat.SepaAccountPayload;
import bisq.account.accounts.fiat.SepaInstantAccount;
import bisq.account.accounts.fiat.SepaInstantAccountPayload;
import bisq.account.accounts.fiat.StrikeAccount;
import bisq.account.accounts.fiat.StrikeAccountPayload;
import bisq.account.accounts.fiat.SwishAccount;
import bisq.account.accounts.fiat.SwishAccountPayload;
import bisq.account.accounts.fiat.UpholdAccount;
import bisq.account.accounts.fiat.UpholdAccountPayload;
import bisq.account.accounts.fiat.UpiAccount;
import bisq.account.accounts.fiat.UpiAccountPayload;
import bisq.account.accounts.fiat.USPostalMoneyOrderAccount;
import bisq.account.accounts.fiat.USPostalMoneyOrderAccountPayload;
import bisq.account.accounts.fiat.VerseAccount;
import bisq.account.accounts.fiat.VerseAccountPayload;
import bisq.account.accounts.fiat.WeChatPayAccount;
import bisq.account.accounts.fiat.WeChatPayAccountPayload;
import bisq.account.accounts.fiat.WiseAccount;
import bisq.account.accounts.fiat.WiseAccountPayload;
import bisq.account.accounts.fiat.WiseUsdAccount;
import bisq.account.accounts.fiat.WiseUsdAccountPayload;
import bisq.account.accounts.fiat.ZelleAccount;
import bisq.account.accounts.fiat.ZelleAccountPayload;
import bisq.account.payment_method.PaymentMethod;
import bisq.account.payment_method.crypto.CryptoPaymentRail;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.account.timestamp.KeyAlgorithm;
import bisq.common.asset.FiatCurrencyRepository;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.summary.details.AccountDetailsGridPane;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.summary.details.AchTransferAccountDetailsGridPane;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.summary.details.AdvancedCashAccountDetailsGridPane;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.summary.details.AliPayAccountDetailsGridPane;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.summary.details.AmazonGiftCardAccountDetailsGridPane;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.summary.details.BizumAccountDetailsGridPane;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.summary.details.CashByMailAccountDetailsGridPane;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.summary.details.CashDepositAccountDetailsGridPane;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.summary.details.DomesticWireTransferAccountDetailsGridPane;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.summary.details.F2FAccountDetailsGridPane;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.summary.details.FasterPaymentsAccountDetailsGridPane;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.summary.details.HalCashAccountDetailsGridPane;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.summary.details.ImpsAccountDetailsGridPane;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.summary.details.InteracETransferAccountDetailsGridPane;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.summary.details.MercadoPagoAccountDetailsGridPane;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.summary.details.MoneseAccountDetailsGridPane;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.summary.details.MoneyBeamAccountDetailsGridPane;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.summary.details.MoneyGramAccountDetailsGridPane;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.summary.details.NationalBankAccountDetailsGridPane;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.summary.details.NeftAccountDetailsGridPane;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.summary.details.PayIdAccountDetailsGridPane;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.summary.details.PayseraAccountDetailsGridPane;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.summary.details.PerfectMoneyAccountDetailsGridPane;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.summary.details.Pin4AccountDetailsGridPane;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.summary.details.PixAccountDetailsGridPane;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.summary.details.PromptPayAccountDetailsGridPane;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.summary.details.RevolutAccountDetailsGridPane;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.summary.details.SameBankAccountDetailsGridPane;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.summary.details.SatispayAccountDetailsGridPane;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.summary.details.SbpAccountDetailsGridPane;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.summary.details.SepaAccountDetailsGridPane;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.summary.details.SepaInstantAccountDetailsGridPane;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.summary.details.StrikeAccountDetailsGridPane;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.summary.details.SwishAccountDetailsGridPane;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.summary.details.UpholdAccountDetailsGridPane;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.summary.details.UpiAccountDetailsGridPane;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.summary.details.USPostalMoneyOrderAccountDetailsGridPane;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.summary.details.VerseAccountDetailsGridPane;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.summary.details.WeChatPayAccountDetailsGridPane;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.summary.details.WiseAccountDetailsGridPane;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.summary.details.WiseUsdAccountDetailsGridPane;
import bisq.desktop.main.content.user.accounts.fiat_accounts.create.summary.details.ZelleAccountDetailsGridPane;
import bisq.desktop.overlay.OverlayController;
import bisq.i18n.Res;
import bisq.security.keys.KeyGeneration;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class PaymentSummaryController implements Controller {
    private final PaymentSummaryModel model;
    @Getter
    private final PaymentSummaryView view;
    private final AccountService accountService;

    public PaymentSummaryController(ServiceProvider serviceProvider) {
        accountService = serviceProvider.getAccountService();
        model = new PaymentSummaryModel();
        view = new PaymentSummaryView(model, this);
    }

    public void setPaymentMethod(PaymentMethod<?> paymentMethod) {
        checkNotNull(paymentMethod, "PaymentMethod must not be null");
        model.setPaymentMethod(paymentMethod);
    }

    public void setAccountPayload(AccountPayload<?> accountPayload) {
        model.setAccountPayload(accountPayload);
        model.setDefaultAccountName(accountPayload.getDefaultAccountName());

        String currencyString = switch (accountPayload) {
            case MultiCurrencyAccountPayload multiCurrencyAccountPayload ->
                    FiatCurrencyRepository.getCodeAndDisplayNames(multiCurrencyAccountPayload.getSelectedCurrencyCodes());
            case SelectableCurrencyAccountPayload selectableCurrencyAccountPayload ->
                    FiatCurrencyRepository.getCodeAndDisplayName(selectableCurrencyAccountPayload.getSelectedCurrencyCode());
            case SingleCurrencyAccountPayload singleCurrencyAccountPayload ->
                    FiatCurrencyRepository.getCodeAndDisplayName(singleCurrencyAccountPayload.getCurrencyCode());
            case null, default ->
                    throw new UnsupportedOperationException("accountPayload of unexpected type: " + accountPayload.getClass().getSimpleName());
        };
        model.setCurrencyString(currencyString);

        if (accountPayload instanceof CountryBasedAccountPayload countryBasedAccountPayload) {
            model.setCountry(countryBasedAccountPayload.getCountry().getName());
        }

        if (model.getPaymentMethod().getPaymentRail() instanceof FiatPaymentRail fiatPaymentRail) {
            AccountDetailsGridPane<?, ?> accountDetailsGridPane = getAccountDetailsGridPane(accountPayload, fiatPaymentRail);
            model.setAccountDetailsGridPane(accountDetailsGridPane);
        }
    }

    public void showAccountNameOverlay() {
        model.getShowAccountNameOverlay().set(true);
    }

    public void onCreateAccount(String accountName) {
        if (model.getAccountNameValidator().hasErrors()) {
            new Popup().invalid(model.getAccountNameValidator().getMessage())
                    .owner(view.getRoot())
                    .show();
            return;
        }
        Set<String> existingNames = accountService.getAccounts().stream()
                .map(Account::getAccountName)
                .collect(Collectors.toSet());
        if (existingNames.contains(accountName)) {
            new Popup().warning(Res.get("paymentAccounts.summary.accountNameOverlay.accountName.error.nameAlreadyExists")).show();
            return;
        }

        Account<? extends PaymentMethod<?>, ?> account = getAccount(accountName);
        accountService.addPaymentAccount(account);
        model.getShowAccountNameOverlay().set(false);
        OverlayController.hide();
    }

    @Override
    public void onActivate() {
        model.getShowAccountNameOverlay().set(false);
    }

    @Override
    public void onDeactivate() {
    }

    private AccountDetailsGridPane<?, ?> getAccountDetailsGridPane(AccountPayload<?> accountPayload,
                                                                   FiatPaymentRail fiatPaymentRail) {
        return switch (fiatPaymentRail) {
            case ACH_TRANSFER ->
                    new AchTransferAccountDetailsGridPane((AchTransferAccountPayload) accountPayload, fiatPaymentRail);
            case ADVANCED_CASH ->
                    new AdvancedCashAccountDetailsGridPane((AdvancedCashAccountPayload) accountPayload, fiatPaymentRail);
            case ALI_PAY ->
                    new AliPayAccountDetailsGridPane((AliPayAccountPayload) accountPayload, fiatPaymentRail);
            case AMAZON_GIFT_CARD ->
                    new AmazonGiftCardAccountDetailsGridPane((AmazonGiftCardAccountPayload) accountPayload, fiatPaymentRail);
            case BIZUM ->
                    new BizumAccountDetailsGridPane((BizumAccountPayload) accountPayload, fiatPaymentRail);
            case CASH_APP -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
            case CASH_BY_MAIL ->
                    new CashByMailAccountDetailsGridPane((CashByMailAccountPayload) accountPayload, fiatPaymentRail);
            case CASH_DEPOSIT ->
                    new CashDepositAccountDetailsGridPane((CashDepositAccountPayload) accountPayload, fiatPaymentRail);
            case CUSTOM -> throw new UnsupportedOperationException("FiatPaymentRail.CUSTOM is not supported");
            case DOMESTIC_WIRE_TRANSFER ->
                    new DomesticWireTransferAccountDetailsGridPane((DomesticWireTransferAccountPayload) accountPayload, fiatPaymentRail);
            case F2F -> new F2FAccountDetailsGridPane((F2FAccountPayload) accountPayload, fiatPaymentRail);
            case FASTER_PAYMENTS ->
                    new FasterPaymentsAccountDetailsGridPane((FasterPaymentsAccountPayload) accountPayload, fiatPaymentRail);
            case HAL_CASH ->
                    new HalCashAccountDetailsGridPane((HalCashAccountPayload) accountPayload, fiatPaymentRail);
            case IMPS ->
                    new ImpsAccountDetailsGridPane((ImpsAccountPayload) accountPayload, fiatPaymentRail);
            case INTERAC_E_TRANSFER ->
                    new InteracETransferAccountDetailsGridPane((InteracETransferAccountPayload) accountPayload, fiatPaymentRail);
            case MERCADO_PAGO ->
                    new MercadoPagoAccountDetailsGridPane((MercadoPagoAccountPayload) accountPayload, fiatPaymentRail);
            case MONESE ->
                    new MoneseAccountDetailsGridPane((MoneseAccountPayload) accountPayload, fiatPaymentRail);
            case MONEY_BEAM ->
                    new MoneyBeamAccountDetailsGridPane((MoneyBeamAccountPayload) accountPayload, fiatPaymentRail);
            case MONEY_GRAM ->
                    new MoneyGramAccountDetailsGridPane((MoneyGramAccountPayload) accountPayload, fiatPaymentRail);
            case NATIONAL_BANK ->
                    new NationalBankAccountDetailsGridPane((NationalBankAccountPayload) accountPayload, fiatPaymentRail);
            case NEFT ->
                    new NeftAccountDetailsGridPane((NeftAccountPayload) accountPayload, fiatPaymentRail);
            case PAY_ID ->
                    new PayIdAccountDetailsGridPane((PayIdAccountPayload) accountPayload, fiatPaymentRail);
            case PAYSERA ->
                    new PayseraAccountDetailsGridPane((PayseraAccountPayload) accountPayload, fiatPaymentRail);
            case PERFECT_MONEY ->
                    new PerfectMoneyAccountDetailsGridPane((PerfectMoneyAccountPayload) accountPayload, fiatPaymentRail);
            case PIN_4 ->
                    new Pin4AccountDetailsGridPane((Pin4AccountPayload) accountPayload, fiatPaymentRail);
            case PIX -> new PixAccountDetailsGridPane((PixAccountPayload) accountPayload, fiatPaymentRail);
            case PROMPT_PAY ->
                    new PromptPayAccountDetailsGridPane((PromptPayAccountPayload) accountPayload, fiatPaymentRail);
            case REVOLUT -> new RevolutAccountDetailsGridPane((RevolutAccountPayload) accountPayload, fiatPaymentRail);
            case SAME_BANK ->
                    new SameBankAccountDetailsGridPane((SameBankAccountPayload) accountPayload, fiatPaymentRail);
            case SATISPAY ->
                    new SatispayAccountDetailsGridPane((SatispayAccountPayload) accountPayload, fiatPaymentRail);
            case SBP ->
                    new SbpAccountDetailsGridPane((SbpAccountPayload) accountPayload, fiatPaymentRail);
            case SEPA -> new SepaAccountDetailsGridPane((SepaAccountPayload) accountPayload, fiatPaymentRail);
            case SEPA_INSTANT ->
                    new SepaInstantAccountDetailsGridPane((SepaInstantAccountPayload) accountPayload, fiatPaymentRail);
            case STRIKE ->
                    new StrikeAccountDetailsGridPane((StrikeAccountPayload) accountPayload, fiatPaymentRail);
            case SWIFT -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
            case SWISH ->
                    new SwishAccountDetailsGridPane((SwishAccountPayload) accountPayload, fiatPaymentRail);
            case UPHOLD ->
                    new UpholdAccountDetailsGridPane((UpholdAccountPayload) accountPayload, fiatPaymentRail);
            case UPI ->
                    new UpiAccountDetailsGridPane((UpiAccountPayload) accountPayload, fiatPaymentRail);
            case US_POSTAL_MONEY_ORDER ->
                    new USPostalMoneyOrderAccountDetailsGridPane((USPostalMoneyOrderAccountPayload) accountPayload, fiatPaymentRail);
            case VERSE ->
                    new VerseAccountDetailsGridPane((VerseAccountPayload) accountPayload, fiatPaymentRail);
            case WECHAT_PAY ->
                    new WeChatPayAccountDetailsGridPane((WeChatPayAccountPayload) accountPayload, fiatPaymentRail);
            case WISE ->
                    new WiseAccountDetailsGridPane((WiseAccountPayload) accountPayload, fiatPaymentRail);
            case WISE_USD ->
                    new WiseUsdAccountDetailsGridPane((WiseUsdAccountPayload) accountPayload, fiatPaymentRail);
            case ZELLE -> new ZelleAccountDetailsGridPane((ZelleAccountPayload) accountPayload, fiatPaymentRail);
        };
    }

    private Account<? extends PaymentMethod<?>, ?> getAccount(String accountName) {
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        KeyAlgorithm keyAlgorithm = KeyAlgorithm.EC;
        AccountOrigin accountOrigin = AccountOrigin.BISQ2_NEW;
        if (model.getPaymentMethod().getPaymentRail() instanceof FiatPaymentRail fiatPaymentRail) {
            return switch (fiatPaymentRail) {
                case ACH_TRANSFER ->
                        new AchTransferAccount(StringUtils.createUid(),
                                System.currentTimeMillis(),
                                accountName,
                                (AchTransferAccountPayload) model.getAccountPayload(),
                                keyPair,
                                keyAlgorithm,
                                accountOrigin);
                case ADVANCED_CASH ->
                        new AdvancedCashAccount(StringUtils.createUid(),
                                System.currentTimeMillis(),
                                accountName,
                                (AdvancedCashAccountPayload) model.getAccountPayload(),
                                keyPair,
                                keyAlgorithm,
                                accountOrigin);
                case ALI_PAY ->
                        new AliPayAccount(StringUtils.createUid(),
                                System.currentTimeMillis(),
                                accountName,
                                (AliPayAccountPayload) model.getAccountPayload(),
                                keyPair,
                                keyAlgorithm,
                                accountOrigin);
                case AMAZON_GIFT_CARD ->
                        new AmazonGiftCardAccount(StringUtils.createUid(),
                                System.currentTimeMillis(),
                                accountName,
                                (AmazonGiftCardAccountPayload) model.getAccountPayload(),
                                keyPair,
                                keyAlgorithm,
                                accountOrigin);
                case BIZUM ->
                        new BizumAccount(StringUtils.createUid(),
                                System.currentTimeMillis(),
                                accountName,
                                (BizumAccountPayload) model.getAccountPayload(),
                                keyPair,
                                keyAlgorithm,
                                accountOrigin);
                case CASH_APP -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
                case CASH_BY_MAIL ->
                        new CashByMailAccount(StringUtils.createUid(),
                                System.currentTimeMillis(),
                                accountName,
                                (CashByMailAccountPayload) model.getAccountPayload(),
                                keyPair,
                                keyAlgorithm,
                                accountOrigin);
                case CASH_DEPOSIT ->
                        new CashDepositAccount(StringUtils.createUid(),
                                System.currentTimeMillis(),
                                accountName,
                                (CashDepositAccountPayload) model.getAccountPayload(),
                                keyPair,
                                keyAlgorithm,
                                accountOrigin);
                case CUSTOM -> throw new UnsupportedOperationException("FiatPaymentRail.CUSTOM is not supported");
                case DOMESTIC_WIRE_TRANSFER ->
                        new DomesticWireTransferAccount(StringUtils.createUid(),
                                System.currentTimeMillis(),
                                accountName,
                                (DomesticWireTransferAccountPayload) model.getAccountPayload(),
                                keyPair,
                                keyAlgorithm,
                                accountOrigin);
                case F2F ->
                        new F2FAccount(StringUtils.createUid(),
                                System.currentTimeMillis(),
                                accountName,
                                (F2FAccountPayload) model.getAccountPayload(),
                                keyPair,
                                keyAlgorithm,
                                accountOrigin);
                case FASTER_PAYMENTS ->
                        new FasterPaymentsAccount(StringUtils.createUid(),
                                System.currentTimeMillis(),
                                accountName,
                                (FasterPaymentsAccountPayload) model.getAccountPayload(),
                                keyPair,
                                keyAlgorithm,
                                accountOrigin);
                case HAL_CASH ->
                        new HalCashAccount(StringUtils.createUid(),
                                System.currentTimeMillis(),
                                accountName,
                                (HalCashAccountPayload) model.getAccountPayload(),
                                keyPair,
                                keyAlgorithm,
                                accountOrigin);
                case IMPS ->
                        new ImpsAccount(StringUtils.createUid(),
                                System.currentTimeMillis(),
                                accountName,
                                (ImpsAccountPayload) model.getAccountPayload(),
                                keyPair,
                                keyAlgorithm,
                                accountOrigin);
                case INTERAC_E_TRANSFER ->
                        new InteracETransferAccount(StringUtils.createUid(),
                                System.currentTimeMillis(),
                                accountName,
                                (InteracETransferAccountPayload) model.getAccountPayload(),
                                keyPair,
                                keyAlgorithm,
                                accountOrigin);
                case MERCADO_PAGO ->
                        new MercadoPagoAccount(StringUtils.createUid(),
                                System.currentTimeMillis(),
                                accountName,
                                (MercadoPagoAccountPayload) model.getAccountPayload(),
                                keyPair,
                                keyAlgorithm,
                                accountOrigin);
                case MONESE ->
                        new MoneseAccount(StringUtils.createUid(),
                                System.currentTimeMillis(),
                                accountName,
                                (MoneseAccountPayload) model.getAccountPayload(),
                                keyPair,
                                keyAlgorithm,
                                accountOrigin);
                case MONEY_BEAM ->
                        new MoneyBeamAccount(StringUtils.createUid(),
                                System.currentTimeMillis(),
                                accountName,
                                (MoneyBeamAccountPayload) model.getAccountPayload(),
                                keyPair,
                                keyAlgorithm,
                                accountOrigin);
                case MONEY_GRAM ->
                        new MoneyGramAccount(StringUtils.createUid(),
                                System.currentTimeMillis(),
                                accountName,
                                (MoneyGramAccountPayload) model.getAccountPayload(),
                                keyPair,
                                keyAlgorithm,
                                accountOrigin);
                case NATIONAL_BANK ->
                        new NationalBankAccount(StringUtils.createUid(),
                                System.currentTimeMillis(),
                                accountName,
                                (NationalBankAccountPayload) model.getAccountPayload(),
                                keyPair,
                                keyAlgorithm,
                                accountOrigin);
                case NEFT ->
                        new NeftAccount(StringUtils.createUid(),
                                System.currentTimeMillis(),
                                accountName,
                                (NeftAccountPayload) model.getAccountPayload(),
                                keyPair,
                                keyAlgorithm,
                                accountOrigin);
                case PAY_ID ->
                        new PayIdAccount(StringUtils.createUid(),
                                System.currentTimeMillis(),
                                accountName,
                                (PayIdAccountPayload) model.getAccountPayload(),
                                keyPair,
                                keyAlgorithm,
                                accountOrigin);
                case PAYSERA ->
                        new PayseraAccount(StringUtils.createUid(),
                                System.currentTimeMillis(),
                                accountName,
                                (PayseraAccountPayload) model.getAccountPayload(),
                                keyPair,
                                keyAlgorithm,
                                accountOrigin);
                case PERFECT_MONEY ->
                        new PerfectMoneyAccount(StringUtils.createUid(),
                                System.currentTimeMillis(),
                                accountName,
                                (PerfectMoneyAccountPayload) model.getAccountPayload(),
                                keyPair,
                                keyAlgorithm,
                                accountOrigin);
                case PIN_4 ->
                        new Pin4Account(StringUtils.createUid(),
                                System.currentTimeMillis(),
                                accountName,
                                (Pin4AccountPayload) model.getAccountPayload(),
                                keyPair,
                                keyAlgorithm,
                                accountOrigin);
                case PIX ->
                        new PixAccount(StringUtils.createUid(),
                                System.currentTimeMillis(),
                                accountName,
                                (PixAccountPayload) model.getAccountPayload(),
                                keyPair,
                                keyAlgorithm,
                                accountOrigin);
                case PROMPT_PAY ->
                        new PromptPayAccount(StringUtils.createUid(),
                                System.currentTimeMillis(),
                                accountName,
                                (PromptPayAccountPayload) model.getAccountPayload(),
                                keyPair,
                                keyAlgorithm,
                                accountOrigin);
                case REVOLUT ->
                        new RevolutAccount(StringUtils.createUid(),
                                System.currentTimeMillis(),
                                accountName,
                                (RevolutAccountPayload) model.getAccountPayload(),
                                keyPair,
                                keyAlgorithm,
                                accountOrigin);
                case SAME_BANK ->
                        new SameBankAccount(StringUtils.createUid(),
                                System.currentTimeMillis(),
                                accountName,
                                (SameBankAccountPayload) model.getAccountPayload(),
                                keyPair,
                                keyAlgorithm,
                                accountOrigin);
                case SATISPAY ->
                        new SatispayAccount(StringUtils.createUid(),
                                System.currentTimeMillis(),
                                accountName,
                                (SatispayAccountPayload) model.getAccountPayload(),
                                keyPair,
                                keyAlgorithm,
                                accountOrigin);
                case SBP ->
                        new SbpAccount(StringUtils.createUid(),
                                System.currentTimeMillis(),
                                accountName,
                                (SbpAccountPayload) model.getAccountPayload(),
                                keyPair,
                                keyAlgorithm,
                                accountOrigin);
                case SEPA ->
                        new SepaAccount(StringUtils.createUid(),
                                System.currentTimeMillis(),
                                accountName,
                                (SepaAccountPayload) model.getAccountPayload(),
                                keyPair,
                                keyAlgorithm,
                                accountOrigin);
                case SEPA_INSTANT ->
                        new SepaInstantAccount(StringUtils.createUid(),
                                System.currentTimeMillis(),
                                accountName,
                                (SepaInstantAccountPayload) model.getAccountPayload(),
                                keyPair,
                                keyAlgorithm,
                                accountOrigin);
                case STRIKE ->
                        new StrikeAccount(StringUtils.createUid(),
                                System.currentTimeMillis(),
                                accountName,
                                (StrikeAccountPayload) model.getAccountPayload(),
                                keyPair,
                                keyAlgorithm,
                                accountOrigin);
                case SWIFT -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
                case SWISH ->
                        new SwishAccount(StringUtils.createUid(),
                                System.currentTimeMillis(),
                                accountName,
                                (SwishAccountPayload) model.getAccountPayload(),
                                keyPair,
                                keyAlgorithm,
                                accountOrigin);
                case UPHOLD ->
                        new UpholdAccount(StringUtils.createUid(),
                                System.currentTimeMillis(),
                                accountName,
                                (UpholdAccountPayload) model.getAccountPayload(),
                                keyPair,
                                keyAlgorithm,
                                accountOrigin);
                case UPI ->
                        new UpiAccount(StringUtils.createUid(),
                                System.currentTimeMillis(),
                                accountName,
                                (UpiAccountPayload) model.getAccountPayload(),
                                keyPair,
                                keyAlgorithm,
                                accountOrigin);
                case US_POSTAL_MONEY_ORDER ->
                        new USPostalMoneyOrderAccount(StringUtils.createUid(),
                                System.currentTimeMillis(),
                                accountName,
                                (USPostalMoneyOrderAccountPayload) model.getAccountPayload(),
                                keyPair,
                                keyAlgorithm,
                                accountOrigin);
                case VERSE ->
                        new VerseAccount(StringUtils.createUid(),
                                System.currentTimeMillis(),
                                accountName,
                                (VerseAccountPayload) model.getAccountPayload(),
                                keyPair,
                                keyAlgorithm,
                                accountOrigin);
                case WECHAT_PAY ->
                        new WeChatPayAccount(StringUtils.createUid(),
                                System.currentTimeMillis(),
                                accountName,
                                (WeChatPayAccountPayload) model.getAccountPayload(),
                                keyPair,
                                keyAlgorithm,
                                accountOrigin);
                case WISE ->
                        new WiseAccount(StringUtils.createUid(),
                                System.currentTimeMillis(),
                                accountName,
                                (WiseAccountPayload) model.getAccountPayload(),
                                keyPair,
                                keyAlgorithm,
                                accountOrigin);
                case WISE_USD ->
                        new WiseUsdAccount(StringUtils.createUid(),
                                System.currentTimeMillis(),
                                accountName,
                                (WiseUsdAccountPayload) model.getAccountPayload(),
                                keyPair,
                                keyAlgorithm,
                                accountOrigin);
                case ZELLE ->
                        new ZelleAccount(StringUtils.createUid(),
                                System.currentTimeMillis(),
                                accountName,
                                (ZelleAccountPayload) model.getAccountPayload(),
                                keyPair,
                                keyAlgorithm,
                                accountOrigin);
            };
        } else if (model.getPaymentMethod().getPaymentRail() instanceof CryptoPaymentRail cryptoPaymentRail) {
            throw new UnsupportedOperationException("Unsupported paymentRail " + model.getPaymentMethod().getPaymentRail());
            /*
            return switch (cryptoPaymentRail) {
                case CUSTOM -> throw new UnsupportedOperationException("Not yet implemented:  " + cryptoPaymentRail);
                case BSQ -> throw new UnsupportedOperationException("Not yet implemented:  " + cryptoPaymentRail);
                case MONERO -> throw new UnsupportedOperationException("Not yet implemented:  " + cryptoPaymentRail);
                case LIQUID -> throw new UnsupportedOperationException("Not yet implemented:  " + cryptoPaymentRail);
                case NATIVE_CHAIN -> throw new UnsupportedOperationException("Not yet implemented:  " + cryptoPaymentRail);
                case ATOMIC_SWAP_CAPABLE_CHAIN -> throw new UnsupportedOperationException("Not yet implemented:  " + cryptoPaymentRail);
                case OTHER -> throw new UnsupportedOperationException("Not yet implemented:  " + cryptoPaymentRail);
            };*/
        } else {
            throw new UnsupportedOperationException("Unsupported paymentRail " + model.getPaymentMethod().getPaymentRail());
        }
    }
}
