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
import bisq.account.accounts.AccountPayload;
import bisq.account.accounts.MultiCurrencyAccountPayload;
import bisq.account.accounts.SelectableCurrencyAccountPayload;
import bisq.account.accounts.SingleCurrencyAccountPayload;
import bisq.account.accounts.fiat.UserDefinedFiatAccount;
import bisq.account.payment_method.PaymentMethodUtil;
import bisq.account.payment_method.PaymentMethod;
import bisq.common.market.Market;
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
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class MuSigCreateOfferPaymentController implements Controller {
    private static final int MAX_NUM_PAYMENT_METHODS = 4;

    private final MuSigCreateOfferPaymentModel model;
    @Getter
    private final MuSigCreateOfferPaymentView view;
    private final Region owner;
    private final AccountService accountService;
    private final Consumer<Boolean> navigationButtonsVisibleHandler;
    private Subscription paymentMethodWithoutAccountPin, paymentMethodWithMultipleAccountsPin;

    public MuSigCreateOfferPaymentController(ServiceProvider serviceProvider,
                                             Region owner,
                                             Consumer<Boolean> navigationButtonsVisibleHandler) {
        accountService = serviceProvider.getAccountService();
        this.owner = owner;
        this.navigationButtonsVisibleHandler = navigationButtonsVisibleHandler;

        model = new MuSigCreateOfferPaymentModel();
        view = new MuSigCreateOfferPaymentView(model, this);
        model.getSortedAccountsForPaymentMethod().setComparator(Comparator.comparing(Account::getAccountName));
    }

    public ReadOnlyObservableMap<PaymentMethod<?>, Account<?, ?>> getSelectedAccountByPaymentMethod() {
        return model.getSelectedAccountByPaymentMethod();
    }

    public List<Account<?, ?>> getEligibleAccounts() {
        String currencyCode = model.getPaymentMethodCurrencyCode();
        if (currencyCode == null) return List.of();

        return accountService.getAccounts().stream()
                .filter(account -> !(account instanceof UserDefinedFiatAccount))
                .filter(account ->
                        account.getAccountPayload().getSelectedCurrencyCodes().stream()
                                .anyMatch(code -> code.equals(currencyCode)))
                                .collect(Collectors.toList());
    }

    public boolean validate() {
        if (model.getSelectedAccountByPaymentMethod().isEmpty()) {
            navigationButtonsVisibleHandler.accept(false);
            model.getShouldShowNoPaymentMethodSelectedOverlay().set(true);
            model.getNoPaymentMethodSelectedOverlayText().set(
                    model.getMarket().get().isCrypto()
                            ? Res.get("muSig.createOffer.paymentMethods.noPaymentMethodSelectedOverlay.cryptoAsset.subTitle")
                            : Res.get("muSig.createOffer.paymentMethods.noPaymentMethodSelectedOverlay.fiat.subTitle"));
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
        model.setPaymentMethodCurrencyCode(market.isCrypto() ? market.getBaseCurrencyCode() : market.getQuoteCurrencyCode());
    }

    public void reset() {
        model.reset();
    }

    @Override
    public void onActivate() {
        model.getSortedPaymentMethods().setComparator(Comparator.comparing(PaymentMethod::getShortDisplayString));
        model.getPaymentMethods().setAll(PaymentMethodUtil.getPaymentMethods(model.getPaymentMethodCurrencyCode()));
        model.getAccountsByPaymentMethod().putAll(getEligibleAccounts().stream()
                .collect(Collectors.groupingBy(
                        Account::getPaymentMethod,
                        Collectors.toList()
                )));
        model.getPaymentMethodWithoutAccount().set(null);
        model.getPaymentMethodWithMultipleAccounts().set(null);

        paymentMethodWithoutAccountPin = EasyBind.subscribe(model.getPaymentMethodWithoutAccount(), paymentMethod -> {
            if (paymentMethod != null) {
                model.getNoAccountOverlayHeadlineText().set(
                        Res.get("muSig.createOffer.paymentMethod.noAccountOverlay.headline",
                                paymentMethod.getShortDisplayString()));
                updateShouldShowNoAccountOverlay(true);
            }
        });
        paymentMethodWithMultipleAccountsPin = EasyBind.subscribe(model.getPaymentMethodWithMultipleAccounts(), paymentMethod -> {
            if (paymentMethod != null) {
                model.getMultipleAccountsOverlayHeadlineText().set(
                        Res.get("muSig.createOffer.paymentMethod.multipleAccountOverlay.headline",
                                paymentMethod.getShortDisplayString()));
                updateShouldShowMultipleAccountsOverlay(true);
            }
        });
    }

    @Override
    public void onDeactivate() {
        model.getAccountsByPaymentMethod().clear();
        updateShouldShowNoAccountOverlay(false);
        updateShouldShowMultipleAccountsOverlay(false);

        paymentMethodWithoutAccountPin.unsubscribe();
        paymentMethodWithMultipleAccountsPin.unsubscribe();
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

    public void selectAccount(Account<? extends PaymentMethod<?>, ?> account, PaymentMethod<?> paymentMethod) {
        doSelectAccount(account, paymentMethod);
    }

    void onSelectAccount(Account<? extends PaymentMethod<?>, ?> account, PaymentMethod<?> paymentMethod) {
        doSelectAccount(account, paymentMethod);
    }

    private void doSelectAccount(Account<? extends PaymentMethod<?>, ?> account, PaymentMethod<?> paymentMethod) {
        if (account != null) {
            model.getSelectedAccountByPaymentMethod().put(paymentMethod, account);
            model.getPaymentMethodWithMultipleAccounts().set(null);
        }
        updateShouldShowMultipleAccountsOverlay(false);
    }

    void onOpenCreateAccountScreen() {
        onCloseNoAccountOverlay();
        OverlayController.hide(() -> Navigation.navigateTo(NavigationTarget.FIAT_PAYMENT_ACCOUNTS));
    }

    void onCloseNoAccountOverlay() {
        PaymentMethod<?> paymentMethod = model.getPaymentMethodWithoutAccount().get();
        if (paymentMethod != null) {
            model.getSelectedPaymentMethods().remove(paymentMethod);
        }
        model.getPaymentMethodWithoutAccount().set(null);
        updateShouldShowNoAccountOverlay(false);
    }

    void onKeyPressedWhileShowingNoAccountOverlay(KeyEvent keyEvent) {
        KeyHandlerUtil.handleEnterKeyEvent(keyEvent, this::onOpenCreateAccountScreen);
        KeyHandlerUtil.handleEscapeKeyEvent(keyEvent, this::onCloseNoAccountOverlay);
    }

    void onCloseMultipleAccountsOverlay() {
        PaymentMethod<?> paymentMethod = model.getPaymentMethodWithMultipleAccounts().get();
        if (paymentMethod != null) {
            model.getSelectedPaymentMethods().remove(paymentMethod);
        }
        model.getPaymentMethodWithMultipleAccounts().set(null);
        updateShouldShowMultipleAccountsOverlay(false);
    }

    void onKeyPressedWhileShowingMultipleAccountsOverlay(KeyEvent keyEvent) {
        KeyHandlerUtil.handleEnterKeyEvent(keyEvent, () -> {});
        KeyHandlerUtil.handleEscapeKeyEvent(keyEvent, this::onCloseMultipleAccountsOverlay);
    }

    void onCloseNoPaymentMethodSelectedOverlay() {
        if (model.getShouldShowNoPaymentMethodSelectedOverlay().get()) {
            navigationButtonsVisibleHandler.accept(true);
            model.getShouldShowNoPaymentMethodSelectedOverlay().set(false);
        }
    }

    void onKeyPressedWhileShowingNoPaymentMethodSelectedOverlay(KeyEvent keyEvent) {
        KeyHandlerUtil.handleEnterKeyEvent(keyEvent, () -> {
        });
        KeyHandlerUtil.handleEscapeKeyEvent(keyEvent, this::onCloseNoPaymentMethodSelectedOverlay);
    }

    private void updateShouldShowNoAccountOverlay(boolean shouldShow) {
        navigationButtonsVisibleHandler.accept(!shouldShow);
        model.getShouldShowNoAccountOverlay().set(shouldShow);
    }

    private void updateShouldShowMultipleAccountsOverlay(boolean shouldShow) {
        navigationButtonsVisibleHandler.accept(!shouldShow);
        model.getShouldShowMultipleAccountsOverlay().set(shouldShow);
    }

    private static List<String> getAccountCurrencyCodes(AccountPayload<? extends PaymentMethod<?>> accountPayload) {
        return switch (accountPayload) {
            case MultiCurrencyAccountPayload multiCurrencyAccountPayload ->
                    multiCurrencyAccountPayload.getSelectedCurrencyCodes();
            case SelectableCurrencyAccountPayload selectableCurrencyAccountPayload ->
                    Collections.singletonList(selectableCurrencyAccountPayload.getSelectedCurrencyCode());
            case SingleCurrencyAccountPayload singleCurrencyAccountPayload ->
                    Collections.singletonList(singleCurrencyAccountPayload.getCurrencyCode());
            case null, default -> {
                log.error("accountPayload of unexpected type: {}",
                        (accountPayload != null
                                ? accountPayload.getClass().getSimpleName()
                                : "accountPayload is null"));
                yield List.of();
            }
        };
    }
}
