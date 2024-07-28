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

package bisq.desktop.main.content.bisq_easy.trade_wizard.payment_methods;

import bisq.account.payment_method.*;
import bisq.common.currency.Market;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import bisq.offer.Direction;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;
import com.google.common.base.Joiner;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.layout.Region;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class TradeWizardPaymentMethodsController implements Controller {
    private static final BitcoinPaymentMethod ON_CHAIN_PAYMENT_METHOD = BitcoinPaymentMethod.fromPaymentRail(BitcoinPaymentRail.MAIN_CHAIN);
    private static final BitcoinPaymentMethod LN_PAYMENT_METHOD = BitcoinPaymentMethod.fromPaymentRail(BitcoinPaymentRail.LN);
    private static final int MAX_ALLOWED_CUSTOM_FIAT_PAYMENTS = 3;
    private static final int MAX_ALLOWED_SELECTED_FIAT_PAYMENTS = 4;

    private final TradeWizardPaymentMethodsModel model;
    @Getter
    private final TradeWizardPaymentMethodsView view;
    private final SettingsService settingsService;
    private final Runnable onNextHandler;
    private final Region owner;
    private Subscription isLNMethodAllowedPin;
    private ListChangeListener<BitcoinPaymentMethod> selectedBitcoinPaymentMethodsListener;
    private ListChangeListener<FiatPaymentMethod> addedCustomFiatPaymentMethodsListener;

    public TradeWizardPaymentMethodsController(ServiceProvider serviceProvider, Region owner, Runnable onNextHandler) {
        settingsService = serviceProvider.getSettingsService();
        this.onNextHandler = onNextHandler;
        this.owner = owner;

        model = new TradeWizardPaymentMethodsModel();
        view = new TradeWizardPaymentMethodsView(model, this);
    }

    public ObservableList<FiatPaymentMethod> getFiatPaymentMethods() {
        return model.getSelectedFiatPaymentMethods();
    }

    public ObservableList<BitcoinPaymentMethod> getBitcoinPaymentMethods() {
        return model.getSelectedBitcoinPaymentMethods();
    }

    public boolean validate() {
        if (getCustomFiatPaymentMethodNameNotEmpty()) {
            return tryAddCustomFiatPaymentMethodAndNavigateNext();
        }
        if (model.getSelectedFiatPaymentMethods().isEmpty()) {
            new Popup().invalid(Res.get("bisqEasy.tradeWizard.paymentMethod.warn.noPaymentMethodSelected"))
                    .owner(owner)
                    .show();
            return false;
        } else {
            return true;
        }
    }

    public boolean getCustomFiatPaymentMethodNameNotEmpty() {
        return StringUtils.isNotEmpty(model.getCustomFiatPaymentMethodName().get());
    }

    public boolean tryAddCustomFiatPaymentMethodAndNavigateNext() {
        if (doAddCustomFiatMethod()) {
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
        model.getSelectedFiatPaymentMethods().clear();
        model.getFiatPaymentMethods().setAll(FiatPaymentMethodUtil.getPaymentMethods(market.getQuoteCurrencyCode()));
        model.getFiatPaymentMethods().addAll(model.getAddedCustomFiatPaymentMethods());
        model.getIsPaymentMethodsEmpty().set(model.getFiatPaymentMethods().isEmpty());
    }

    public void reset() {
        model.reset();
    }

    @Override
    public void onActivate() {
        model.setFiatSubtitleLabel(model.getDirection().isBuy()
                ? Res.get("bisqEasy.tradeWizard.fiatPaymentMethods.subTitle.buyer", model.getMarket().get().getQuoteCurrencyCode())
                : Res.get("bisqEasy.tradeWizard.fiatPaymentMethods.subTitle.seller", model.getMarket().get().getQuoteCurrencyCode()));
        model.setBitcoinSubtitleLabel(model.getDirection().isBuy()
                ? Res.get("bisqEasy.tradeWizard.bitcoinPaymentMethods.subTitle.buyer")
                : Res.get("bisqEasy.tradeWizard.bitcoinPaymentMethods.subTitle.seller"));
        model.getCustomFiatPaymentMethodName().set("");
        model.getSortedFiatPaymentMethods().setComparator(Comparator.comparing(PaymentMethod::getShortDisplayString));

        List<BitcoinPaymentMethod> paymentMethods = Stream.of(BitcoinPaymentRail.MAIN_CHAIN, BitcoinPaymentRail.LN)
                .map(BitcoinPaymentMethod::fromPaymentRail)
                .collect(Collectors.toList());
        model.getBitcoinPaymentMethods().setAll(paymentMethods);
        model.getSortedBitcoinPaymentMethods().setComparator(Comparator.comparingInt(o -> o.getPaymentRail().ordinal()));

        selectedBitcoinPaymentMethodsListener = change -> updateIsLNMethodAllowed();
        model.getSelectedBitcoinPaymentMethods().addListener(selectedBitcoinPaymentMethodsListener);
        updateIsLNMethodAllowed();

        addedCustomFiatPaymentMethodsListener = change -> updateCanAddCustomFiatPaymentMethod();
        model.getAddedCustomFiatPaymentMethods().addListener(addedCustomFiatPaymentMethodsListener);
        updateCanAddCustomFiatPaymentMethod();

        maybeRemoveCustomFiatPaymentMethods();

        settingsService.getCookie().asString(CookieKey.CREATE_OFFER_METHODS, getCookieSubKey())
                .ifPresent(names -> {
                    List.of(names.split(",")).forEach(name -> {
                        if (name.isEmpty()) {
                            return;
                        }
                        FiatPaymentMethod fiatPaymentMethod = FiatPaymentMethodUtil.getPaymentMethod(name);
                        boolean isCustomPaymentMethod = fiatPaymentMethod.isCustomPaymentMethod();
                        if (!isCustomPaymentMethod && isPredefinedPaymentMethodsContainName(name)) {
                            maybeAddFiatPaymentMethod(fiatPaymentMethod);
                        } else {
                            maybeAddCustomFiatPaymentMethod(fiatPaymentMethod);
                        }
                    });
                });
        settingsService.getCookie().asString(CookieKey.CREATE_OFFER_BITCOIN_METHODS)
                .ifPresent(names -> {
                    List.of(names.split(",")).forEach(name -> {
                        if (name.isEmpty()) {
                            return;
                        }
                        BitcoinPaymentMethod bitcoinPaymentMethod = BitcoinPaymentMethodUtil.getPaymentMethod(name);
                        maybeAddBitcoinPaymentMethod(bitcoinPaymentMethod);
                    });
                });
        maybeAddOnChainPaymentMethodAsSelected();

        isLNMethodAllowedPin = EasyBind.subscribe(model.getIsLNMethodAllowed(), isLNAllowed ->
           onToggleBitcoinPaymentMethod(LN_PAYMENT_METHOD, isLNAllowed));
    }

    @Override
    public void onDeactivate() {
        model.getSelectedBitcoinPaymentMethods().removeListener(selectedBitcoinPaymentMethodsListener);
        model.getAddedCustomFiatPaymentMethods().removeListener(addedCustomFiatPaymentMethodsListener);
        isLNMethodAllowedPin.unsubscribe();
    }

    boolean onToggleFiatPaymentMethod(FiatPaymentMethod fiatPaymentMethod, boolean isSelected) {
        if (isSelected) {
            if (model.getSelectedFiatPaymentMethods().size() >= MAX_ALLOWED_SELECTED_FIAT_PAYMENTS) {
                new Popup().warning(Res.get("bisqEasy.tradeWizard.paymentMethod.warn.maxMethodsReached")).show();
                return false;
            }
            if (fiatPaymentMethod.isCustomPaymentMethod()) {
                maybeAddCustomFiatPaymentMethod(fiatPaymentMethod);
            } else {
                maybeAddFiatPaymentMethod(fiatPaymentMethod);
            }
        } else {
            model.getSelectedFiatPaymentMethods().remove(fiatPaymentMethod);
            setCreateOfferFiatMethodsCookie();
        }
        return true;
    }

    void onAddCustomFiatMethod() {
        doAddCustomFiatMethod();
    }

    private boolean doAddCustomFiatMethod() {
        if (model.getSelectedFiatPaymentMethods().size() >= MAX_ALLOWED_SELECTED_FIAT_PAYMENTS) {
            new Popup().warning(Res.get("bisqEasy.tradeWizard.paymentMethod.warn.maxMethodsReached")).show();
            return false;
        }
        String customName = model.getCustomFiatPaymentMethodName().get();
        if (customName == null || customName.isBlank() || customName.trim().isEmpty()) {
            return false;
        }
        if (customName.length() > 20) {
            new Popup().warning(Res.get("bisqEasy.tradeWizard.paymentMethod.warn.tooLong")).show();
            return false;
        }
        FiatPaymentMethod customFiatPaymentMethod = FiatPaymentMethod.fromCustomName(customName);
        if (model.getAddedCustomFiatPaymentMethods().contains(customFiatPaymentMethod)) {
            new Popup().warning(Res.get("bisqEasy.tradeWizard.paymentMethod.warn.customPaymentMethodAlreadyExists", customFiatPaymentMethod.getName())).show();
            return false;
        }
        return maybeAddCustomFiatPaymentMethod(customFiatPaymentMethod);
    }

    private void maybeAddFiatPaymentMethod(FiatPaymentMethod fiatPaymentMethod) {
        if (!model.getSelectedFiatPaymentMethods().contains(fiatPaymentMethod)) {
            model.getSelectedFiatPaymentMethods().add(fiatPaymentMethod);
            setCreateOfferFiatMethodsCookie();
        }
        if (!model.getFiatPaymentMethods().contains(fiatPaymentMethod)) {
            model.getFiatPaymentMethods().add(fiatPaymentMethod);
        }
    }

    private boolean maybeAddCustomFiatPaymentMethod(FiatPaymentMethod fiatPaymentMethod) {
        if (fiatPaymentMethod != null) {
            if (!model.getAddedCustomFiatPaymentMethods().contains(fiatPaymentMethod)) {
                String customName = fiatPaymentMethod.getName().toUpperCase().strip();
                if (isPredefinedPaymentMethodsContainName(customName)) {
                    new Popup().warning(Res.get("bisqEasy.tradeWizard.paymentMethod.warn.customNameMatchesPredefinedMethod")).show();
                    model.getCustomFiatPaymentMethodName().set("");
                    return false;
                }
                model.getAddedCustomFiatPaymentMethods().add(fiatPaymentMethod);
            } else {
                return false;
            }
            maybeAddFiatPaymentMethod(fiatPaymentMethod);
            model.getCustomFiatPaymentMethodName().set("");
            return true;
        }
        return false;
    }

    private boolean isPredefinedPaymentMethodsContainName(String name) {
        return new HashSet<>(PaymentMethodUtil.getPaymentMethodNames(model.getFiatPaymentMethods())).contains(name);
    }

    void onRemoveFiatCustomMethod(FiatPaymentMethod fiatPaymentMethod) {
        model.getAddedCustomFiatPaymentMethods().remove(fiatPaymentMethod);
        model.getSelectedFiatPaymentMethods().remove(fiatPaymentMethod);
        model.getFiatPaymentMethods().remove(fiatPaymentMethod);
        setCreateOfferFiatMethodsCookie();
    }

    private void setCreateOfferFiatMethodsCookie() {
        settingsService.setCookie(CookieKey.CREATE_OFFER_METHODS, getCookieSubKey(),
                Joiner.on(",").join(PaymentMethodUtil.getPaymentMethodNames(model.getSelectedFiatPaymentMethods())));
    }

    private String getCookieSubKey() {
        return model.getMarket().get().getMarketCodes();
    }

    boolean onToggleBitcoinPaymentMethod(BitcoinPaymentMethod bitcoinPaymentMethod, boolean isSelected) {
        if (isSelected) {
            if (model.getSelectedBitcoinPaymentMethods().size() >= 4) {
                new Popup().warning(Res.get("bisqEasy.tradeWizard.paymentMethod.warn.maxMethodsReached")).show();
                return false;
            }
            maybeAddBitcoinPaymentMethod(bitcoinPaymentMethod);
        } else {
            model.getSelectedBitcoinPaymentMethods().remove(bitcoinPaymentMethod);
            setCreateOfferBitcoinMethodsCookie();
        }
        return true;
    }

    void onRemoveCustomBitcoinMethod(BitcoinPaymentMethod bitcoinPaymentMethod) {
        model.getAddedCustomBitcoinPaymentMethods().remove(bitcoinPaymentMethod);
        model.getSelectedBitcoinPaymentMethods().remove(bitcoinPaymentMethod);
        model.getBitcoinPaymentMethods().remove(bitcoinPaymentMethod);
        setCreateOfferBitcoinMethodsCookie();
    }

    private void maybeAddBitcoinPaymentMethod(BitcoinPaymentMethod bitcoinPaymentMethod) {
        if (!model.getSelectedBitcoinPaymentMethods().contains(bitcoinPaymentMethod)) {
            model.getSelectedBitcoinPaymentMethods().add(bitcoinPaymentMethod);
            setCreateOfferBitcoinMethodsCookie();
        }
        if (!model.getBitcoinPaymentMethods().contains(bitcoinPaymentMethod)) {
            model.getBitcoinPaymentMethods().add(bitcoinPaymentMethod);
        }
    }

    private void setCreateOfferBitcoinMethodsCookie() {
        settingsService.setCookie(CookieKey.CREATE_OFFER_BITCOIN_METHODS,
                Joiner.on(",").join(PaymentMethodUtil.getPaymentMethodNames(model.getSelectedBitcoinPaymentMethods())));
    }

    private void maybeRemoveCustomFiatPaymentMethods() {
        // To ensure backwards compatibility we need to drop custom fiat payment methods if the user has more than 3,
        // which is the max allowed number of custom fiat payment methods per market
        while (model.getAddedCustomFiatPaymentMethods().size() > MAX_ALLOWED_CUSTOM_FIAT_PAYMENTS) {
            model.getAddedCustomFiatPaymentMethods().remove(model.getAddedCustomBitcoinPaymentMethods().size() - 1);
        }
    }

    private void updateIsLNMethodAllowed() {
        boolean isLNSelected = model.getSelectedBitcoinPaymentMethods().contains(LN_PAYMENT_METHOD);
        model.getIsLNMethodAllowed().set(isLNSelected);
    }

    private void updateCanAddCustomFiatPaymentMethod() {
        model.getCanAddCustomFiatPaymentMethod().set(model.getAddedCustomFiatPaymentMethods().size() < MAX_ALLOWED_CUSTOM_FIAT_PAYMENTS);
    }

    private void maybeAddOnChainPaymentMethodAsSelected() {
        if (model.getSelectedFiatPaymentMethods().isEmpty()) {
            model.getSelectedBitcoinPaymentMethods().add(ON_CHAIN_PAYMENT_METHOD);
        }
    }
}
