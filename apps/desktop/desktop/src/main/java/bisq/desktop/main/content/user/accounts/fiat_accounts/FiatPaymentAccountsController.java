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

package bisq.desktop.main.content.user.accounts.fiat_accounts;

import bisq.account.AccountService;
import bisq.account.accounts.Account;
import bisq.account.accounts.AccountPayload;
import bisq.account.accounts.crypto.CryptoAssetAccount;
import bisq.account.accounts.fiat.AchTransferAccount;
import bisq.account.accounts.fiat.AdvancedCashAccount;
import bisq.account.accounts.fiat.AliPayAccount;
import bisq.account.accounts.fiat.AmazonGiftCardAccount;
import bisq.account.accounts.fiat.BizumAccount;
import bisq.account.accounts.fiat.CashByMailAccount;
import bisq.account.accounts.fiat.CashDepositAccount;
import bisq.account.accounts.fiat.DomesticWireTransferAccount;
import bisq.account.accounts.fiat.F2FAccount;
import bisq.account.accounts.fiat.FasterPaymentsAccount;
import bisq.account.accounts.fiat.HalCashAccount;
import bisq.account.accounts.fiat.ImpsAccount;
import bisq.account.accounts.fiat.InteracETransferAccount;
import bisq.account.accounts.fiat.MercadoPagoAccount;
import bisq.account.accounts.fiat.MoneseAccount;
import bisq.account.accounts.fiat.MoneyBeamAccount;
import bisq.account.accounts.fiat.MoneyGramAccount;
import bisq.account.accounts.fiat.NationalBankAccount;
import bisq.account.accounts.fiat.NeftAccount;
import bisq.account.accounts.fiat.PayIdAccount;
import bisq.account.accounts.fiat.PayseraAccount;
import bisq.account.accounts.fiat.PerfectMoneyAccount;
import bisq.account.accounts.fiat.Pin4Account;
import bisq.account.accounts.fiat.PixAccount;
import bisq.account.accounts.fiat.PromptPayAccount;
import bisq.account.accounts.fiat.RevolutAccount;
import bisq.account.accounts.fiat.SameBankAccount;
import bisq.account.accounts.fiat.SatispayAccount;
import bisq.account.accounts.fiat.SbpAccount;
import bisq.account.accounts.fiat.SepaAccount;
import bisq.account.accounts.fiat.SepaInstantAccount;
import bisq.account.accounts.fiat.StrikeAccount;
import bisq.account.accounts.fiat.SwishAccount;
import bisq.account.accounts.fiat.UpholdAccount;
import bisq.account.accounts.fiat.UpiAccount;
import bisq.account.accounts.fiat.USPostalMoneyOrderAccount;
import bisq.account.accounts.fiat.UserDefinedFiatAccount;
import bisq.account.accounts.fiat.UserDefinedFiatAccountPayload;
import bisq.account.accounts.fiat.VerseAccount;
import bisq.account.accounts.fiat.WeChatPayAccount;
import bisq.account.accounts.fiat.WiseAccount;
import bisq.account.accounts.fiat.WiseUsdAccount;
import bisq.account.accounts.fiat.ZelleAccount;
import bisq.account.payment_method.PaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.account.timestamp.AccountTimestampService;
import bisq.account.timestamp.KeyAlgorithm;
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
import bisq.desktop.main.content.user.accounts.fiat_accounts.details.AchTransferAccountDetails;
import bisq.desktop.main.content.user.accounts.fiat_accounts.details.AdvancedCashAccountDetails;
import bisq.desktop.main.content.user.accounts.fiat_accounts.details.AliPayAccountDetails;
import bisq.desktop.main.content.user.accounts.fiat_accounts.details.AmazonGiftCardAccountDetails;
import bisq.desktop.main.content.user.accounts.fiat_accounts.details.BizumAccountDetails;
import bisq.desktop.main.content.user.accounts.fiat_accounts.details.CashByMailAccountDetails;
import bisq.desktop.main.content.user.accounts.fiat_accounts.details.CashDepositAccountDetails;
import bisq.desktop.main.content.user.accounts.fiat_accounts.details.DomesticWireTransferAccountDetails;
import bisq.desktop.main.content.user.accounts.fiat_accounts.details.F2FAccountDetails;
import bisq.desktop.main.content.user.accounts.fiat_accounts.details.FasterPaymentsAccountDetails;
import bisq.desktop.main.content.user.accounts.fiat_accounts.details.HalCashAccountDetails;
import bisq.desktop.main.content.user.accounts.fiat_accounts.details.ImpsAccountDetails;
import bisq.desktop.main.content.user.accounts.fiat_accounts.details.InteracETransferAccountDetails;
import bisq.desktop.main.content.user.accounts.fiat_accounts.details.MercadoPagoAccountDetails;
import bisq.desktop.main.content.user.accounts.fiat_accounts.details.MoneseAccountDetails;
import bisq.desktop.main.content.user.accounts.fiat_accounts.details.MoneyBeamAccountDetails;
import bisq.desktop.main.content.user.accounts.fiat_accounts.details.MoneyGramAccountDetails;
import bisq.desktop.main.content.user.accounts.fiat_accounts.details.NationalBankAccountDetails;
import bisq.desktop.main.content.user.accounts.fiat_accounts.details.NeftAccountDetails;
import bisq.desktop.main.content.user.accounts.fiat_accounts.details.PayIdAccountDetails;
import bisq.desktop.main.content.user.accounts.fiat_accounts.details.PayseraAccountDetails;
import bisq.desktop.main.content.user.accounts.fiat_accounts.details.PerfectMoneyAccountDetails;
import bisq.desktop.main.content.user.accounts.fiat_accounts.details.Pin4AccountDetails;
import bisq.desktop.main.content.user.accounts.fiat_accounts.details.PixAccountDetails;
import bisq.desktop.main.content.user.accounts.fiat_accounts.details.PromptPayAccountDetails;
import bisq.desktop.main.content.user.accounts.fiat_accounts.details.RevolutAccountDetails;
import bisq.desktop.main.content.user.accounts.fiat_accounts.details.SameBankAccountDetails;
import bisq.desktop.main.content.user.accounts.fiat_accounts.details.SatispayAccountDetails;
import bisq.desktop.main.content.user.accounts.fiat_accounts.details.SbpAccountDetails;
import bisq.desktop.main.content.user.accounts.fiat_accounts.details.SepaAccountDetails;
import bisq.desktop.main.content.user.accounts.fiat_accounts.details.SepaInstantAccountDetails;
import bisq.desktop.main.content.user.accounts.fiat_accounts.details.StrikeAccountDetails;
import bisq.desktop.main.content.user.accounts.fiat_accounts.details.SwishAccountDetails;
import bisq.desktop.main.content.user.accounts.fiat_accounts.details.UpholdAccountDetails;
import bisq.desktop.main.content.user.accounts.fiat_accounts.details.UpiAccountDetails;
import bisq.desktop.main.content.user.accounts.fiat_accounts.details.USPostalMoneyOrderAccountDetails;
import bisq.desktop.main.content.user.accounts.fiat_accounts.details.UserDefinedAccountDetails;
import bisq.desktop.main.content.user.accounts.fiat_accounts.details.VerseAccountDetails;
import bisq.desktop.main.content.user.accounts.fiat_accounts.details.WeChatPayAccountDetails;
import bisq.desktop.main.content.user.accounts.fiat_accounts.details.WiseAccountDetails;
import bisq.desktop.main.content.user.accounts.fiat_accounts.details.WiseUsdAccountDetails;
import bisq.desktop.main.content.user.accounts.fiat_accounts.details.ZelleAccountDetails;
import bisq.desktop.navigation.NavigationTarget;
import bisq.mu_sig.MuSigService;
import javafx.beans.property.ReadOnlyStringProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import javax.annotation.Nullable;
import java.security.KeyPair;
import java.util.Comparator;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class FiatPaymentAccountsController implements Controller {
    private final FiatPaymentAccountsModel model;
    @Getter
    private final FiatPaymentAccountsView view;
    private final AccountService accountService;
    private final MuSigService muSigService;
    private final AccountTimestampService accountTimestampService;
    private Subscription selectedAccountSubscription;
    private Pin accountsPin, selectedAccountPin;
    @Nullable
    private Subscription userDefinedAccountDetailsPin;

    public FiatPaymentAccountsController(ServiceProvider serviceProvider) {
        accountService = serviceProvider.getAccountService();
        muSigService = serviceProvider.getMuSigService();
        accountTimestampService = accountService.getAccountTimestampService();

        model = new FiatPaymentAccountsModel();
        view = new FiatPaymentAccountsView(model, this);

        model.getSortedAccounts().setComparator(Comparator.comparing(Account::getAccountName));
    }

    @Override
    public void onActivate() {
        model.getImportBisq1AccountDataButtonVisible().setValue(muSigService.getMuSigActivated().get());

        accountsPin = accountService.getAccountByNameMap().addObserver(new HashMapObserver<>() {
            @Override
            public void put(String key, Account<? extends PaymentMethod<?>, ?> account) {
                UIThread.run(() -> {
                    if (!(account instanceof CryptoAssetAccount) &&
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
                        return;
                    }

                    accountService.setSelectedAccount(selectedAccount);
                    applyDataDisplay(selectedAccount);

                    boolean isUserDefinedFiatAccount = selectedAccount instanceof UserDefinedFiatAccount;
                    model.getSaveUserDefinedFiatAccountButtonVisible().set(isUserDefinedFiatAccount && !accountService.getFiatAccounts().isEmpty());

                    if (UserDefinedAccountDetails.USE_LEGACY_DESIGN) {
                        disposeUserDefinedAccountDetailsPin();
                        if (UserDefinedAccountDetails.USE_LEGACY_DESIGN && selectedAccount instanceof UserDefinedFiatAccount userDefinedFiatAccount) {
                            AccountDetails<?, ?> accountDetails = model.getAccountDetails().get();
                            if (accountDetails instanceof UserDefinedAccountDetails userDefinedAccountDetails) {
                                ReadOnlyStringProperty textAreaTextProperty = userDefinedAccountDetails.getTextAreaTextProperty();
                                if (textAreaTextProperty != null) {
                                    String accountData = textAreaTextProperty.get();
                                    userDefinedAccountDetailsPin = EasyBind.subscribe(textAreaTextProperty, newValue -> {
                                        String oldValue = userDefinedFiatAccount.getAccountPayload().getAccountData();
                                        model.getSaveUserDefinedFiatAccountButtonDisabled().set(StringUtils.isEmpty(newValue) || StringUtils.isEmpty(oldValue) || oldValue.equals(newValue));
                                    });
                                }
                            }
                        }
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
        disposeUserDefinedAccountDetailsPin();
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
            Navigation.navigateTo(NavigationTarget.CREATE_PAYMENT_ACCOUNT);
        } else {
            Navigation.navigateTo(NavigationTarget.CREATE_PAYMENT_ACCOUNT_LEGACY);
        }
    }

    void onSaveUserDefinedFiatAccount() {
        var selectedAccount = model.getSelectedAccount().get();
        if (selectedAccount instanceof UserDefinedFiatAccount userDefinedFiatAccount) {
            AccountDetails<?, ?> accountDetails = model.getAccountDetails().get();
            if (accountDetails instanceof UserDefinedAccountDetails userDefinedAccountDetails) {
                ReadOnlyStringProperty textAreaTextProperty = userDefinedAccountDetails.getTextAreaTextProperty();
                String accountData = textAreaTextProperty != null ? textAreaTextProperty.get() : null;
                if (StringUtils.isEmpty(accountData)) {
                    return;
                }
                checkArgument(accountData.length() <= UserDefinedFiatAccountPayload.MAX_DATA_LENGTH, "Account data must not be longer than 1000 characters");

                String accountId = userDefinedFiatAccount.getId();
                long creationDate = userDefinedFiatAccount.getCreationDate();
                String accountName = userDefinedFiatAccount.getAccountName();
                KeyPair keyPair = userDefinedFiatAccount.getKeyPair();
                KeyAlgorithm keyAlgorithm = userDefinedFiatAccount.getKeyAlgorithm();
                UserDefinedFiatAccountPayload newAccountPayload = new UserDefinedFiatAccountPayload(accountId, accountData);
                UserDefinedFiatAccount newAccount = new UserDefinedFiatAccount(accountId,
                        creationDate,
                        accountName,
                        newAccountPayload,
                        keyPair,
                        keyAlgorithm,
                        userDefinedFiatAccount.getAccountOrigin());
                accountService.removePaymentAccount(selectedAccount);
                accountService.addPaymentAccount(newAccount);
                accountService.setSelectedAccount(newAccount);
            }
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
        boolean hasNoAccounts = accountService.getFiatAccounts().isEmpty();
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

        AccountPayload<? extends PaymentMethod<?>> accountPayload = account.getAccountPayload();
        if (account.getPaymentMethod().getPaymentRail() instanceof FiatPaymentRail fiatPaymentRail) {
            try {
                AccountDetails<?, ?> accountDetails = getAccountDetails(account, fiatPaymentRail);
                model.getAccountDetails().set(accountDetails);
            } catch (Exception e) {
                log.warn("Could not resolve account details", e);
            }
        }
    }

    private AccountDetails<?, ?> getAccountDetails(Account<? extends PaymentMethod<?>, ?> account,
                                                   FiatPaymentRail fiatPaymentRail) {
        return switch (fiatPaymentRail) {
            case ACH_TRANSFER ->
                    new AchTransferAccountDetails((AchTransferAccount) account, accountTimestampService);
            case ADVANCED_CASH ->
                    new AdvancedCashAccountDetails((AdvancedCashAccount) account, accountTimestampService);
            case ALI_PAY ->
                    new AliPayAccountDetails((AliPayAccount) account, accountTimestampService);
            case AMAZON_GIFT_CARD ->
                    new AmazonGiftCardAccountDetails((AmazonGiftCardAccount) account, accountTimestampService);
            case BIZUM ->
                    new BizumAccountDetails((BizumAccount) account, accountTimestampService);
            case CASH_APP -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
            case CASH_BY_MAIL ->
                    new CashByMailAccountDetails((CashByMailAccount) account, accountTimestampService);
            case CASH_DEPOSIT ->
                    new CashDepositAccountDetails((CashDepositAccount) account, accountTimestampService);
            case CUSTOM -> new UserDefinedAccountDetails((UserDefinedFiatAccount) account, accountTimestampService);
            case DOMESTIC_WIRE_TRANSFER ->
                    new DomesticWireTransferAccountDetails((DomesticWireTransferAccount) account, accountTimestampService);
            case F2F -> new F2FAccountDetails((F2FAccount) account, accountTimestampService);
            case FASTER_PAYMENTS ->
                    new FasterPaymentsAccountDetails((FasterPaymentsAccount) account, accountTimestampService);
            case HAL_CASH ->
                    new HalCashAccountDetails((HalCashAccount) account, accountTimestampService);
            case IMPS ->
                    new ImpsAccountDetails((ImpsAccount) account, accountTimestampService);
            case INTERAC_E_TRANSFER ->
                    new InteracETransferAccountDetails((InteracETransferAccount) account, accountTimestampService);
            case MERCADO_PAGO ->
                    new MercadoPagoAccountDetails((MercadoPagoAccount) account, accountTimestampService);
            case MONESE ->
                    new MoneseAccountDetails((MoneseAccount) account, accountTimestampService);
            case MONEY_BEAM ->
                    new MoneyBeamAccountDetails((MoneyBeamAccount) account, accountTimestampService);
            case MONEY_GRAM ->
                    new MoneyGramAccountDetails((MoneyGramAccount) account, accountTimestampService);
            case NATIONAL_BANK ->
                    new NationalBankAccountDetails((NationalBankAccount) account, accountTimestampService);
            case NEFT ->
                    new NeftAccountDetails((NeftAccount) account, accountTimestampService);
            case PAY_ID ->
                    new PayIdAccountDetails((PayIdAccount) account, accountTimestampService);
            case PAYSERA ->
                    new PayseraAccountDetails((PayseraAccount) account, accountTimestampService);
            case PERFECT_MONEY ->
                    new PerfectMoneyAccountDetails((PerfectMoneyAccount) account, accountTimestampService);
            case PIN_4 ->
                    new Pin4AccountDetails((Pin4Account) account, accountTimestampService);
            case PIX -> new PixAccountDetails((PixAccount) account, accountTimestampService);
            case PROMPT_PAY ->
                    new PromptPayAccountDetails((PromptPayAccount) account, accountTimestampService);
            case REVOLUT -> new RevolutAccountDetails((RevolutAccount) account, accountTimestampService);
            case SAME_BANK ->
                    new SameBankAccountDetails((SameBankAccount) account, accountTimestampService);
            case SATISPAY ->
                    new SatispayAccountDetails((SatispayAccount) account, accountTimestampService);
            case SBP ->
                    new SbpAccountDetails((SbpAccount) account, accountTimestampService);
            case SEPA -> new SepaAccountDetails((SepaAccount) account, accountTimestampService);
            case SEPA_INSTANT ->
                    new SepaInstantAccountDetails((SepaInstantAccount) account, accountTimestampService);
            case STRIKE ->
                    new StrikeAccountDetails((StrikeAccount) account, accountTimestampService);
            case SWIFT -> throw new UnsupportedOperationException("Not yet implemented:  " + fiatPaymentRail);
            case SWISH ->
                    new SwishAccountDetails((SwishAccount) account, accountTimestampService);
            case UPHOLD ->
                    new UpholdAccountDetails((UpholdAccount) account, accountTimestampService);
            case UPI ->
                    new UpiAccountDetails((UpiAccount) account, accountTimestampService);
            case US_POSTAL_MONEY_ORDER ->
                    new USPostalMoneyOrderAccountDetails((USPostalMoneyOrderAccount) account, accountTimestampService);
            case VERSE ->
                    new VerseAccountDetails((VerseAccount) account, accountTimestampService);
            case WECHAT_PAY ->
                    new WeChatPayAccountDetails((WeChatPayAccount) account, accountTimestampService);
            case WISE ->
                    new WiseAccountDetails((WiseAccount) account, accountTimestampService);
            case WISE_USD ->
                    new WiseUsdAccountDetails((WiseUsdAccount) account, accountTimestampService);
            case ZELLE -> new ZelleAccountDetails((ZelleAccount) account, accountTimestampService);
        };
    }

    private Optional<Account<? extends PaymentMethod<?>, ?>> findAccount(String key) {
        return model.getAccounts().stream()
                .filter(account -> account.getAccountName().equals(key))
                .findAny();
    }

    private void disposeUserDefinedAccountDetailsPin() {
        if (userDefinedAccountDetailsPin != null) {
            userDefinedAccountDetailsPin.unsubscribe();
            userDefinedAccountDetailsPin = null;
        }
    }
}
