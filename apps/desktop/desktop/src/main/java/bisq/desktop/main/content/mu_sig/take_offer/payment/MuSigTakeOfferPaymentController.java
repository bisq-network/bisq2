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

package bisq.desktop.main.content.mu_sig.take_offer.payment;

import bisq.account.AccountService;
import bisq.account.accounts.Account;
import bisq.account.accounts.UserDefinedFiatAccount;
import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.account.payment_method.BitcoinPaymentRail;
import bisq.account.payment_method.FiatPaymentMethod;
import bisq.account.payment_method.PaymentMethod;
import bisq.common.currency.Market;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.utils.KeyHandlerUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.navigation.NavigationTarget;
import bisq.desktop.overlay.OverlayController;
import bisq.i18n.Res;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.payment_method.FiatPaymentMethodSpec;
import bisq.offer.payment_method.PaymentMethodSpec;
import bisq.offer.payment_method.PaymentMethodSpecUtil;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class MuSigTakeOfferPaymentController implements Controller {
    private static final BitcoinPaymentMethod MAIN_CHAIN_PAYMENT_METHOD = BitcoinPaymentMethod.fromPaymentRail(BitcoinPaymentRail.MAIN_CHAIN);

    private final MuSigTakeOfferPaymentModel model;
    @Getter
    private final MuSigTakeOfferPaymentView view;
    private final Region owner;
    private final AccountService accountService;

    public MuSigTakeOfferPaymentController(ServiceProvider serviceProvider, Region owner) {
        accountService = serviceProvider.getAccountService();
        this.owner = owner;

        model = new MuSigTakeOfferPaymentModel();
        view = new MuSigTakeOfferPaymentView(model, this);

        model.getSortedAccountsForPaymentMethod().setComparator(Comparator.comparing(Account::getAccountName));
        model.getSortedPaymentMethods().setComparator(Comparator.comparing(PaymentMethod::getShortDisplayString));
    }

    public void init(MuSigOffer muSigOffer) {
        Market market = muSigOffer.getMarket();
        model.setMarket(market);
        model.setDirection(muSigOffer.getDirection());
        String quoteCurrencyCode = market.getQuoteCurrencyCode();
        model.setHeadline(muSigOffer.getTakersDirection().isBuy()
                ? Res.get("muSig.takeOffer.paymentMethods.headline.buyer", quoteCurrencyCode)
                : Res.get("muSig.takeOffer.paymentMethods.headline.seller", quoteCurrencyCode));

        Map<? extends PaymentMethod<?>, List<Account<?, ?>>> accountsByPaymentMethod = accountService.getAccounts().stream()
                .filter(account -> !(account instanceof UserDefinedFiatAccount))
                .filter(account ->
                        account.getAccountPayload().getSelectedCurrencyCodes().contains(quoteCurrencyCode))
                .collect(Collectors.groupingBy(
                        Account::getPaymentMethod,
                        Collectors.toList()
                ));
        model.getAccountsByPaymentMethod().putAll(accountsByPaymentMethod);

        List<FiatPaymentMethodSpec> offeredPaymentMethodSpecs = muSigOffer.getQuoteSidePaymentMethodSpecs();
        boolean isSinglePaymentMethod = offeredPaymentMethodSpecs.size() == 1;
        model.setSinglePaymentMethod(isSinglePaymentMethod);
        if (isSinglePaymentMethod) {
            FiatPaymentMethod paymentMethod = offeredPaymentMethodSpecs.get(0).getPaymentMethod();
            model.getSelectedPaymentMethodSpec().set(PaymentMethodSpecUtil.createPaymentMethodSpec(paymentMethod, model.getMarket().getQuoteCurrencyCode()));

            List<Account<?, ?>> accountsForPaymentMethod = accountsByPaymentMethod.get(paymentMethod);
            checkNotNull(accountsForPaymentMethod, "There must be a account list for paymentMethod " + paymentMethod);
            model.getAccountsForPaymentMethod().setAll(accountsForPaymentMethod);

            model.setSubtitle(Res.get("muSig.takeOffer.paymentMethods.subtitle.account", paymentMethod.getShortDisplayString()));
            model.setSinglePaymentMethodAccountSelectionDescription(Res.get("muSig.takeOffer.paymentMethods.singlePaymentMethod.accountSelection.prompt",
                    paymentMethod.getShortDisplayString()));
        } else {
            model.setSubtitle(Res.get("muSig.takeOffer.paymentMethods.subtitle.paymentMethod"));
        }

        List<? extends PaymentMethod<?>> offeredPaymentMethods = offeredPaymentMethodSpecs.stream()
                .map(spec -> (PaymentMethod<?>) spec.getPaymentMethod())
                .collect(Collectors.toList());
        model.getOfferedPaymentMethods().setAll(offeredPaymentMethods);
    }

    public ReadOnlyObjectProperty<Account<?, ?>> getSelectedAccount() {
        return model.getSelectedAccount();
    }

    public ReadOnlyObjectProperty<PaymentMethodSpec<?>> getPaymentMethodSpec() {
        return model.getSelectedPaymentMethodSpec();
    }

    public boolean validate() {
        if (model.getSelectedAccount().get() == null) {
            new Popup().invalid(Res.get("bisqEasy.tradeWizard.paymentMethods.warn.noFiatPaymentMethodSelected"))
                    .owner(owner)
                    .show();
            return false;
        }

        return true;
    }

    public void reset() {
        model.reset();
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    void onTogglePaymentMethod(PaymentMethod<?> paymentMethod, boolean isSelected) {
        if (paymentMethod == null) {
            return;
        }
        if (isSelected) {
            if (model.getAccountsByPaymentMethod().containsKey(paymentMethod)) {
                model.getSelectedPaymentMethodSpec().set(PaymentMethodSpecUtil.createPaymentMethodSpec(paymentMethod, model.getMarket().getQuoteCurrencyCode()));
                List<Account<?, ?>> accountsForPaymentMethod = model.getAccountsByPaymentMethod().get(paymentMethod);
                checkArgument(!accountsForPaymentMethod.isEmpty());

                if (accountsForPaymentMethod.size() == 1) {
                    model.getSelectedAccount().set(accountsForPaymentMethod.get(0));
                } else {
                    model.getAccountsForPaymentMethod().setAll(accountsForPaymentMethod);
                    model.getPaymentMethodWithMultipleAccounts().set(paymentMethod);
                }
            } else {
                model.getPaymentMethodWithoutAccount().set(paymentMethod);
            }
        } else {
            model.getPaymentMethodWithMultipleAccounts().set(null);
            model.getSelectedAccount().set(null);
            model.getSelectedPaymentMethodSpec().set(null);
            model.getToggleGroup().selectToggle(null);
        }
    }

    void onSelectAccount(Account<? extends PaymentMethod<?>, ?> account) {
        if (account != null) {
            model.getSelectedAccount().set(account);
            model.getPaymentMethodWithMultipleAccounts().set(null);
        }
    }

    void onCloseMultipleAccountsOverlay() {
        model.getPaymentMethodWithMultipleAccounts().set(null);
        model.getSelectedPaymentMethodSpec().set(null);
        model.getSelectedAccount().set(null);
        model.getToggleGroup().selectToggle(null);
    }

    void onOpenCreateAccountScreen(PaymentMethod<?> paymentMethod) {
        model.getPaymentMethodWithoutAccount().set(null);
        model.getSelectedPaymentMethodSpec().set(null);
        model.getToggleGroup().selectToggle(null);
        OverlayController.hide();
        Navigation.navigateTo(NavigationTarget.PAYMENT_ACCOUNTS);
    }

    void onCloseNoAccountOverlay(PaymentMethod<?> paymentMethod) {
        model.getPaymentMethodWithoutAccount().set(null);
        model.getSelectedPaymentMethodSpec().set(null);
        model.getToggleGroup().selectToggle(null);
    }

    void onKeyPressedWhileShowingOverlay(KeyEvent keyEvent) {
        KeyHandlerUtil.handleEnterKeyEvent(keyEvent, () -> {
        });
        KeyHandlerUtil.handleEscapeKeyEvent(keyEvent, () -> {
            model.getPaymentMethodWithoutAccount().set(null);
            model.getPaymentMethodWithMultipleAccounts().set(null);
        });
    }
}
