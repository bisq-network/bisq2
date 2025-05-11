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

package bisq.desktop.main.content.mu_sig.create_offer.payment_methods;

import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.account.payment_method.BitcoinPaymentRail;
import bisq.account.payment_method.FiatPaymentMethod;
import bisq.account.payment_method.FiatPaymentMethodUtil;
import bisq.account.payment_method.NationalCurrencyPaymentMethod;
import bisq.account.payment_method.PaymentMethod;
import bisq.account.payment_method.PaymentMethodUtil;
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

import java.util.Comparator;
import java.util.List;

@Slf4j
public class MuSigCreateOfferPaymentMethodsController implements Controller {
    private static final BitcoinPaymentMethod MAIN_CHAIN_PAYMENT_METHOD = BitcoinPaymentMethod.fromPaymentRail(BitcoinPaymentRail.MAIN_CHAIN);
    private static final int MAX_ALLOWED_CUSTOM_PAYMENT_METHODS = 3;
    private static final int MAX_ALLOWED_SELECTED_PAYMENT_METHODS = 4;

    private final MuSigCreateOfferPaymentMethodsModel model;
    @Getter
    private final MuSigCreateOfferPaymentMethodsView view;
    private final SettingsService settingsService;
    private final Runnable onNextHandler;
    private final Region owner;
    private ListChangeListener<NationalCurrencyPaymentMethod<?>> addedCustomPaymentMethodsListener;

    public MuSigCreateOfferPaymentMethodsController(ServiceProvider serviceProvider,
                                                    Region owner,
                                                    Runnable onNextHandler) {
        settingsService = serviceProvider.getSettingsService();
        this.onNextHandler = onNextHandler;
        this.owner = owner;

        model = new MuSigCreateOfferPaymentMethodsModel();
        view = new MuSigCreateOfferPaymentMethodsView(model, this);
    }

    public ObservableList<FiatPaymentMethod> getPaymentMethods() {
        return model.getSelectedPaymentMethods();
    }

    public boolean validate() {
        if (getCustomPaymentMethodNameNotEmpty()) {
            return tryAddCustomPaymentMethodAndNavigateNext();
        }
        if (model.getSelectedPaymentMethods().isEmpty()) {
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
        model.setSubtitleLabel(model.getDirection().isBuy()
                ? Res.get("bisqEasy.tradeWizard.paymentMethods.fiat.subTitle.buyer", model.getMarket().get().getQuoteCurrencyCode())
                : Res.get("bisqEasy.tradeWizard.paymentMethods.fiat.subTitle.seller", model.getMarket().get().getQuoteCurrencyCode()));
        model.getCustomPaymentMethodName().set("");
        model.getSortedPaymentMethods().setComparator(Comparator.comparing(PaymentMethod::getShortDisplayString));

        addedCustomPaymentMethodsListener = change -> updateCanAddCustomPaymentMethod();
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
                    FiatPaymentMethod paymentMethod;
                    paymentMethod = FiatPaymentMethodUtil.getPaymentMethod(name);
                  /*  if (stablecoinPaymentMethod.isEmpty()) {
                        paymentMethod = FiatPaymentMethodUtil.getPaymentMethod(name);
                    } else {
                        paymentMethod = stablecoinPaymentMethod.get();
                    }*/
                    boolean isCustomPaymentMethod = paymentMethod.isCustomPaymentMethod();
                    if (!isCustomPaymentMethod && isPredefinedPaymentMethodsContainName(name)) {
                        maybeAddPaymentMethod(paymentMethod);
                    } else if (isCustomPaymentMethod) {
                        maybeAddCustomPaymentMethod(paymentMethod);
                    }
                }));
    }

    @Override
    public void onDeactivate() {
        model.getAddedCustomPaymentMethods().removeListener(addedCustomPaymentMethodsListener);
    }

    boolean onTogglePaymentMethod(FiatPaymentMethod paymentMethod, boolean isSelected) {
        if (isSelected) {
            if (model.getSelectedPaymentMethods().size() >= MAX_ALLOWED_SELECTED_PAYMENT_METHODS) {
                new Popup().warning(Res.get("bisqEasy.tradeWizard.paymentMethods.warn.maxMethodsReached")).show();
                return false;
            }
            if (paymentMethod.isCustomPaymentMethod()) {
                maybeAddCustomPaymentMethod(paymentMethod);
            } else {
                maybeAddPaymentMethod(paymentMethod);
            }
        } else {
            model.getSelectedPaymentMethods().remove(paymentMethod);
            setCreateOfferMethodsCookie();
        }
        return true;
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
        FiatPaymentMethod customPaymentMethod = FiatPaymentMethod.fromCustomName(customName);
        if (model.getAddedCustomPaymentMethods().contains(customPaymentMethod)) {
            new Popup().warning(Res.get("bisqEasy.tradeWizard.paymentMethods.warn.customPaymentMethodAlreadyExists", customPaymentMethod.getName())).show();
            return false;
        }
        return maybeAddCustomPaymentMethod(customPaymentMethod);
    }

    private void maybeAddPaymentMethod(FiatPaymentMethod paymentMethod) {
        if (!model.getSelectedPaymentMethods().contains(paymentMethod)) {
            model.getSelectedPaymentMethods().add(paymentMethod);
            setCreateOfferMethodsCookie();
        }
        if (!model.getPaymentMethods().contains(paymentMethod)) {
            model.getPaymentMethods().add(paymentMethod);
        }
    }

    private boolean maybeAddCustomPaymentMethod(FiatPaymentMethod paymentMethod) {
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

    private boolean isPredefinedPaymentMethodsContainName(String name) {
        return model.getPaymentMethods().stream()
                .map(PaymentMethod::getName)
                .anyMatch(e -> e.equals(name));
    }

    void onRemoveCustomMethod(FiatPaymentMethod paymentMethod) {
        model.getAddedCustomPaymentMethods().remove(paymentMethod);
        model.getSelectedPaymentMethods().remove(paymentMethod);
        model.getPaymentMethods().remove(paymentMethod);
        setCreateOfferMethodsCookie();
    }

    private void setCreateOfferMethodsCookie() {
        settingsService.setCookie(CookieKey.CREATE_OFFER_METHODS, getCookieSubKey(),
                Joiner.on(",").join(PaymentMethodUtil.getPaymentMethodNames(model.getSelectedPaymentMethods())));
    }

    private String getCookieSubKey() {
        return model.getMarket().get().getMarketCodes();
    }

    private void maybeRemoveCustomPaymentMethods() {
        // To ensure backwards compatibility we need to drop custom payment methods if the user has more than 3,
        // which is the max allowed number of custom payment methods per market
        while (model.getAddedCustomPaymentMethods().size() > MAX_ALLOWED_CUSTOM_PAYMENT_METHODS) {
            FiatPaymentMethod toRemove = model.getAddedCustomPaymentMethods().remove(model.getAddedCustomPaymentMethods().size() - 1);
            onRemoveCustomMethod(toRemove);
        }
    }

    private void updateCanAddCustomPaymentMethod() {
        model.getCanAddCustomPaymentMethod().set(model.getAddedCustomPaymentMethods().size() < MAX_ALLOWED_CUSTOM_PAYMENT_METHODS);
    }
}
