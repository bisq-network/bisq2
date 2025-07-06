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
import bisq.account.payment_method.FiatPaymentMethod;
import bisq.account.payment_method.FiatPaymentMethodUtil;
import bisq.account.payment_method.PaymentMethod;
import bisq.account.payment_method.PaymentMethodUtil;
import bisq.common.currency.Market;
import bisq.common.observable.map.ReadOnlyObservableMap;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.utils.KeyHandlerUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.navigation.NavigationTarget;
import bisq.desktop.overlay.OverlayController;
import bisq.i18n.Res;
import bisq.offer.Direction;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;
import com.google.common.base.Joiner;
import javafx.collections.ListChangeListener;
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
    private static final int MAX_ALLOWED_CUSTOM_PAYMENT_METHODS = 3;
    private static final int MAX_ALLOWED_SELECTED_PAYMENT_METHODS = 4;

    private final MuSigCreateOfferPaymentModel model;
    @Getter
    private final MuSigCreateOfferPaymentView view;
    private final SettingsService settingsService;
    private final Runnable onNextHandler;
    private final Region owner;
    private final AccountService accountService;
    private final ListChangeListener<PaymentMethod<?>> addedCustomPaymentMethodsListener;

    public MuSigCreateOfferPaymentController(ServiceProvider serviceProvider,
                                             Region owner,
                                             Runnable onNextHandler) {
        settingsService = serviceProvider.getSettingsService();
        accountService = serviceProvider.getAccountService();
        this.onNextHandler = onNextHandler;
        this.owner = owner;

        model = new MuSigCreateOfferPaymentModel();
        view = new MuSigCreateOfferPaymentView(model, this);
        model.getSortedAccountsForPaymentMethod().setComparator(Comparator.comparing(Account::getAccountName));

        addedCustomPaymentMethodsListener = change -> updateCanAddCustomPaymentMethod();
    }

  /*  public ObservableList<PaymentMethod<?>> getPaymentMethods() {
        return model.getSelectedPaymentMethods();
    }*/

    public ReadOnlyObservableMap<PaymentMethod<?>, Account<?, ?>> getSelectedAccountByPaymentMethod() {
        return model.getSelectedAccountByPaymentMethod();
    }

    //ObservableHashMap<PaymentMethod<?>, Account<?, ?>> selectedAccountByPaymentMethod

    public boolean validate() {
       /* if (getCustomPaymentMethodNameNotEmpty()) {
            return tryAddCustomPaymentMethodAndNavigateNext();
        }*/
        if (model.getSelectedAccountByPaymentMethod().isEmpty()) {
            new Popup().invalid(Res.get("bisqEasy.tradeWizard.paymentMethods.warn.noFiatPaymentMethodSelected"))
                    .owner(owner)
                    .show();
            return false;
        }

        return true;
    }

    public boolean getCustomPaymentMethodNameNotEmpty() {
        return StringUtils.isNotEmpty(model.getCustomPaymentMethodName().get());
    }

    public boolean tryAddCustomPaymentMethodAndNavigateNext() {
        if (doAddCustomPaymentMethod()) {
            onNextHandler.run();
            return true;
        }
        return false;
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
        //  model.getPaymentMethods().addAll(StablecoinPaymentMethodUtil.getPaymentMethods(market.getQuoteCurrencyCode()));
        model.getPaymentMethods().addAll(model.getAddedCustomPaymentMethods());
        model.getIsPaymentMethodsEmpty().set(model.getPaymentMethods().isEmpty());
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
        model.getCustomPaymentMethodName().set("");
        model.getSortedPaymentMethods().setComparator(Comparator.comparing(PaymentMethod::getShortDisplayString));

        model.getAddedCustomPaymentMethods().addListener(addedCustomPaymentMethodsListener);
        updateCanAddCustomPaymentMethod();

        maybeRemoveCustomPaymentMethods();

        settingsService.getCookie().asString(CookieKey.CREATE_OFFER_METHODS, getCookieSubKey())
                .ifPresent(names -> List.of(names.split(",")).forEach(name -> {
                    if (name.isEmpty()) {
                        return;
                    }

                    //todo
                    // Optional<StablecoinPaymentMethod> stablecoinPaymentMethod = StablecoinPaymentMethodUtil.findPaymentMethod(name);
                    PaymentMethod<?> paymentMethod;
                    paymentMethod = FiatPaymentMethodUtil.getPaymentMethod(name);
                  /*  if (stablecoinPaymentMethod.isEmpty()) {
                        paymentMethod = FiatPaymentMethodUtil.getPaymentMethod(name);
                    } else {
                        paymentMethod = stablecoinPaymentMethod.get();
                    }*/
                    boolean isCustomPaymentMethod = paymentMethod.isCustomPaymentMethod();
                    if (!isCustomPaymentMethod && isPredefinedPaymentMethodsContainName(name)) {
                        // maybeAddPaymentMethod(paymentMethod);
                    } else if (isCustomPaymentMethod) {
                        maybeAddCustomPaymentMethod(paymentMethod);
                    }
                }));
    }

    @Override
    public void onDeactivate() {
        model.getAccountsByPaymentMethod().clear();
        model.getAddedCustomPaymentMethods().removeListener(addedCustomPaymentMethodsListener);
    }

    void onTogglePaymentMethod(PaymentMethod<?> paymentMethod, boolean isSelected) {
        if (isSelected) {
            if (model.getSelectedPaymentMethods().size() >= MAX_ALLOWED_SELECTED_PAYMENT_METHODS) {
                new Popup().warning(Res.get("bisqEasy.tradeWizard.paymentMethods.warn.maxMethodsReached")).show();
                return;
            }

            if (paymentMethod.isCustomPaymentMethod()) {
                maybeAddCustomPaymentMethod(paymentMethod);
            } else {
                maybeAddPaymentMethod(paymentMethod);
            }
        } else {
            model.getSelectedAccountByPaymentMethod().remove(paymentMethod);
            model.getSelectedPaymentMethods().remove(paymentMethod);
           // setCreateOfferMethodsCookie();
        }
    }


    private void maybeAddPaymentMethod(PaymentMethod<?> paymentMethod) {
        if (!model.getSelectedPaymentMethods().contains(paymentMethod)) {
            model.getSelectedPaymentMethods().add(paymentMethod);
            if (model.getAccountsByPaymentMethod().containsKey(paymentMethod)) {
                List<Account<?, ?>> accountsForPaymentMethod = model.getAccountsByPaymentMethod().get(paymentMethod);
                checkArgument(!accountsForPaymentMethod.isEmpty());

                if (accountsForPaymentMethod.size() == 1) {
                    model.getSelectedAccountByPaymentMethod().put(paymentMethod, accountsForPaymentMethod.get(0));
                    //setCreateOfferMethodsCookie();
                } else {
                    model.getAccountsForPaymentMethod().setAll(accountsForPaymentMethod);
                    model.getPaymentMethodWithMultipleAccounts().set(paymentMethod);
                }
            } else {
                model.getPaymentMethodWithoutAccount().set(paymentMethod);
            }
        }
        /*//todo why?
        if (!model.getPaymentMethods().contains(paymentMethod)) {
            model.getPaymentMethods().add(paymentMethod);
        }*/
    }

    void onSelectAccount(Account<? extends PaymentMethod<?>, ?> account, PaymentMethod<?> paymentMethod) {
        if (account != null) {
            //accountService.setSelectedAccount(account);
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

    /// //////////////////

    private boolean maybeAddCustomPaymentMethod(PaymentMethod<?> paymentMethod) {
        if (paymentMethod != null) {
            if (!model.getAddedCustomPaymentMethods().contains(paymentMethod)) {
                String customName = paymentMethod.getName().toUpperCase().strip();
                if (isPredefinedPaymentMethodsContainName(customName)) {
                    new Popup().warning(Res.get("bisqEasy.tradeWizard.paymentMethods.warn.customNameMatchesPredefinedMethod")).show();
                    model.getCustomPaymentMethodName().set("");
                    return false;
                }
                model.getAddedCustomPaymentMethods().add(paymentMethod);
            } else {
                return false;
            }
            maybeAddPaymentMethod(paymentMethod);
            model.getCustomPaymentMethodName().set("");
            return true;
        }
        return false;
    }


    private void maybeRemoveCustomPaymentMethods() {
        // To ensure backwards compatibility we need to drop custom payment methods if the user has more than 3,
        // which is the max allowed number of custom payment methods per market
        while (model.getAddedCustomPaymentMethods().size() > MAX_ALLOWED_CUSTOM_PAYMENT_METHODS) {
            PaymentMethod<?> toRemove = model.getAddedCustomPaymentMethods().remove(model.getAddedCustomPaymentMethods().size() - 1);
            onRemoveCustomMethod(toRemove);
        }
    }

    private void updateCanAddCustomPaymentMethod() {
        model.getCanAddCustomPaymentMethod().set(model.getAddedCustomPaymentMethods().size() < MAX_ALLOWED_CUSTOM_PAYMENT_METHODS);
    }

    void onAddCustomPaymentMethod() {
        doAddCustomPaymentMethod();
    }

    private boolean doAddCustomPaymentMethod() {
        if (model.getSelectedPaymentMethods().size() >= MAX_ALLOWED_SELECTED_PAYMENT_METHODS) {
            new Popup().warning(Res.get("bisqEasy.tradeWizard.paymentMethods.warn.maxMethodsReached")).show();
            return false;
        }
        String customName = model.getCustomPaymentMethodName().get();
        if (customName == null || customName.isBlank() || customName.trim().isEmpty()) {
            return false;
        }
        if (customName.length() > 20) {
            new Popup().warning(Res.get("bisqEasy.tradeWizard.paymentMethods.warn.tooLong")).show();
            return false;
        }
        PaymentMethod<?> customPaymentMethod = FiatPaymentMethod.fromCustomName(customName);
        if (model.getAddedCustomPaymentMethods().contains(customPaymentMethod)) {
            new Popup().warning(Res.get("bisqEasy.tradeWizard.paymentMethods.warn.customPaymentMethodAlreadyExists", customPaymentMethod.getName())).show();
            return false;
        }
        return maybeAddCustomPaymentMethod(customPaymentMethod);
    }

    private String getCookieSubKey() {
        return model.getMarket().get().getMarketCodes();
    }

    private boolean isPredefinedPaymentMethodsContainName(String name) {
        return model.getPaymentMethods().stream()
                .map(PaymentMethod::getName)
                .anyMatch(e -> e.equals(name));
    }

    void onRemoveCustomMethod(PaymentMethod<?> paymentMethod) {
        model.getAddedCustomPaymentMethods().remove(paymentMethod);
        model.getSelectedPaymentMethods().remove(paymentMethod);
        model.getPaymentMethods().remove(paymentMethod);
        setCreateOfferMethodsCookie();
    }

    private void setCreateOfferMethodsCookie() {
        settingsService.setCookie(CookieKey.CREATE_OFFER_METHODS, getCookieSubKey(),
                Joiner.on(",").join(PaymentMethodUtil.getPaymentMethodNames(model.getSelectedPaymentMethods())));
    }

}
