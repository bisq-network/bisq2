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

package bisq.desktop.main.content.mu_sig.create_offer.payment;

import bisq.account.AccountService;
import bisq.account.accounts.Account;
import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.account.payment_method.BitcoinPaymentRail;
import bisq.account.payment_method.FiatPaymentMethodUtil;
import bisq.account.payment_method.PaymentMethod;
import bisq.common.currency.Market;
import bisq.common.observable.map.ReadOnlyObservableMap;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.utils.KeyHandlerUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.mu_sig.components.PaymentMethodChipButton;
import bisq.desktop.navigation.NavigationTarget;
import bisq.desktop.overlay.OverlayController;
import bisq.i18n.Res;
import bisq.offer.Direction;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class MuSigCreateOfferPaymentController implements Controller {
    private static final BitcoinPaymentMethod MAIN_CHAIN_PAYMENT_METHOD = BitcoinPaymentMethod.fromPaymentRail(BitcoinPaymentRail.MAIN_CHAIN);
    private static final int MAX_NUM_PAYMENT_METHODS = 4;

    private final MuSigCreateOfferPaymentModel model;
    @Getter
    private final MuSigCreateOfferPaymentView view;
    private final Region owner;
    private final AccountService accountService;

    public MuSigCreateOfferPaymentController(ServiceProvider serviceProvider,
                                             Region owner) {
        accountService = serviceProvider.getAccountService();
        this.owner = owner;

        model = new MuSigCreateOfferPaymentModel();
        view = new MuSigCreateOfferPaymentView(model, this);
        model.getSortedAccountsForPaymentMethod().setComparator(Comparator.comparing(Account::getAccountName));
    }

    public ReadOnlyObservableMap<PaymentMethod<?>, Account<?, ?>> getSelectedAccountByPaymentMethod() {
        return model.getSelectedAccountByPaymentMethod();
    }

    public boolean validate() {
        if (model.getSelectedAccountByPaymentMethod().isEmpty()) {
            new Popup().invalid(Res.get("bisqEasy.tradeWizard.paymentMethods.warn.noFiatPaymentMethodSelected"))
                    .owner(owner)
                    .show();
            return false;
        }

        return true;
    }

    public void setDirection(Direction direction) {
        if (direction != null) {
            model.setDirection(direction);
        }
    }

    public void setMarket(Market market) {
        if (market == null) {
            return;
        }

        model.getMarket().set(market);
        model.getSelectedPaymentMethods().clear();
        model.getPaymentMethods().setAll(FiatPaymentMethodUtil.getPaymentMethods(market.getQuoteCurrencyCode()));
    }

    public void reset() {
        model.reset();
    }

    @Override
    public void onActivate() {
        model.getAccountsByPaymentMethod().putAll(accountService.getAccounts().stream()
                .collect(Collectors.groupingBy(
                        Account::getPaymentMethod,
                        Collectors.toList()
                )));
        model.setSubtitleLabel(model.getDirection().isBuy()
                ? Res.get("bisqEasy.tradeWizard.paymentMethods.fiat.subTitle.buyer", model.getMarket().get().getQuoteCurrencyCode())
                : Res.get("bisqEasy.tradeWizard.paymentMethods.fiat.subTitle.seller", model.getMarket().get().getQuoteCurrencyCode()));
        model.getSortedPaymentMethods().setComparator(Comparator.comparing(PaymentMethod::getShortDisplayString));
    }

    @Override
    public void onDeactivate() {
        model.getAccountsByPaymentMethod().clear();
    }

    void onTogglePaymentMethod(PaymentMethod<?> paymentMethod, PaymentMethodChipButton button) {
        if (button.isSelected()) {
            if (!model.getSelectedPaymentMethods().contains(paymentMethod)) {
                if (model.getSelectedPaymentMethods().size() >= MAX_NUM_PAYMENT_METHODS) {
                    new Popup().invalid(Res.get("muSig.createOffer.paymentMethods.warn.maxMethodsReached", MAX_NUM_PAYMENT_METHODS))
                            .owner(owner)
                            .onClose(() -> button.setSelected(false))
                            .show();
                    return;
                }

                model.getSelectedPaymentMethods().add(paymentMethod);
                if (model.getAccountsByPaymentMethod().containsKey(paymentMethod)) {
                    List<Account<?, ?>> accountsForPaymentMethod = model.getAccountsByPaymentMethod().get(paymentMethod);
                    checkArgument(!accountsForPaymentMethod.isEmpty());

                    if (accountsForPaymentMethod.size() == 1) {
                        model.getSelectedAccountByPaymentMethod().put(paymentMethod, accountsForPaymentMethod.get(0));
                    } else {
                        model.getAccountsForPaymentMethod().setAll(accountsForPaymentMethod);
                        model.getPaymentMethodWithMultipleAccounts().set(paymentMethod);
                    }
                } else {
                    model.getPaymentMethodWithoutAccount().set(paymentMethod);
                }
            }
        } else {
            model.getSelectedAccountByPaymentMethod().remove(paymentMethod);
            model.getSelectedPaymentMethods().remove(paymentMethod);
        }
    }

    void onSelectAccount(Account<? extends PaymentMethod<?>, ?> account, PaymentMethod<?> paymentMethod) {
        if (account != null) {
            model.getSelectedAccountByPaymentMethod().put(paymentMethod, account);
            model.getPaymentMethodWithMultipleAccounts().set(null);
        }
    }

    void onOpenCreateAccountScreen(PaymentMethod<?> paymentMethod) {
        model.getPaymentMethodWithoutAccount().set(null);
        model.getSelectedPaymentMethods().remove(paymentMethod);
        OverlayController.hide();
        Navigation.navigateTo(NavigationTarget.PAYMENT_ACCOUNTS);
    }

    void onCloseNoAccountOverlay(PaymentMethod<?> paymentMethod) {
        model.getPaymentMethodWithoutAccount().set(null);
        model.getSelectedPaymentMethods().remove(paymentMethod);
    }

    void onCloseMultipleAccountsOverlay(PaymentMethod<?> paymentMethod) {
        model.getPaymentMethodWithMultipleAccounts().set(null);
        model.getSelectedPaymentMethods().remove(paymentMethod);
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
