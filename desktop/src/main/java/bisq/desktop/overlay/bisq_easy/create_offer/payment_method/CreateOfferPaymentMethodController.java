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

package bisq.desktop.overlay.bisq_easy.create_offer.payment_method;

import bisq.account.payment_method.FiatPaymentMethod;
import bisq.account.payment_method.FiatPaymentMethodUtil;
import bisq.account.payment_method.PaymentMethod;
import bisq.account.payment_method.PaymentMethodUtil;
import bisq.common.currency.Market;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;
import com.google.common.base.Joiner;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.collections.ObservableList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

@Slf4j
public class CreateOfferPaymentMethodController implements Controller {
    private final CreateOfferPaymentMethodModel model;
    @Getter
    private final CreateOfferPaymentMethodView view;
    private final SettingsService settingsService;
    private Subscription customMethodPin;

    public CreateOfferPaymentMethodController(ServiceProvider serviceProvider) {
        settingsService = serviceProvider.getSettingsService();

        model = new CreateOfferPaymentMethodModel();
        view = new CreateOfferPaymentMethodView(model, this);
    }

    public ObservableList<FiatPaymentMethod> getFiatPaymentMethods() {
        return model.getSelectedFiatPaymentMethods();
    }

    public boolean getCustomFiatPaymentMethodNameNotEmpty() {
        return StringUtils.isNotEmpty(model.getCustomFiatPaymentMethodName().get());
    }

    public void showCustomMethodNotEmptyWarning() {
        model.getShowCustomMethodNotEmptyWarning().set(true);
    }

    public ReadOnlyBooleanProperty getShowCustomMethodNotEmptyWarning() {
        return model.getShowCustomMethodNotEmptyWarning();
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
        model.getCustomFiatPaymentMethodName().set("");
        model.getShowCustomMethodNotEmptyWarning().set(false);
        model.getSortedFiatPaymentMethods().setComparator(Comparator.comparing(PaymentMethod::getShortDisplayString));
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
        customMethodPin = EasyBind.subscribe(model.getCustomFiatPaymentMethodName(),
                customMethod -> model.getIsAddCustomMethodIconEnabled().set(customMethod != null && !customMethod.isEmpty()));
    }

    @Override
    public void onDeactivate() {
        customMethodPin.unsubscribe();
    }

    boolean onTogglePaymentMethod(FiatPaymentMethod fiatPaymentMethod, boolean isSelected) {
        if (isSelected) {
            if (model.getSelectedFiatPaymentMethods().size() >= 4) {
                new Popup().warning(Res.get("bisqEasy.createOffer.paymentMethod.warn.maxMethodsReached")).show();
                return false;
            }
            maybeAddFiatPaymentMethod(fiatPaymentMethod);
        } else {
            model.getSelectedFiatPaymentMethods().remove(fiatPaymentMethod);
            setCookie();
        }
        return true;
    }

    void onAddCustomMethod() {
        if (model.getSelectedFiatPaymentMethods().size() >= 4) {
            new Popup().warning(Res.get("bisqEasy.createOffer.paymentMethod.warn.maxMethodsReached")).show();
            return;
        }
        maybeAddCustomFiatPaymentMethod(FiatPaymentMethod.fromCustomName(model.getCustomFiatPaymentMethodName().get()));
    }

    void onCloseOverlay() {
        model.getShowCustomMethodNotEmptyWarning().set(false);
    }

    private void maybeAddFiatPaymentMethod(FiatPaymentMethod fiatPaymentMethod) {
        if (!model.getSelectedFiatPaymentMethods().contains(fiatPaymentMethod)) {
            model.getSelectedFiatPaymentMethods().add(fiatPaymentMethod);
            setCookie();
        }
        if (!model.getFiatPaymentMethods().contains(fiatPaymentMethod)) {
            model.getFiatPaymentMethods().add(fiatPaymentMethod);
        }
    }

    private void maybeAddCustomFiatPaymentMethod(FiatPaymentMethod fiatPaymentMethod) {
        if (fiatPaymentMethod != null) {
            if (!model.getAddedCustomFiatPaymentMethods().contains(fiatPaymentMethod)) {
                String customName = fiatPaymentMethod.getName().toUpperCase().strip();
                if (isPredefinedPaymentMethodsContainName(customName)) {
                    new Popup().warning(Res.get("bisqEasy.createOffer.paymentMethod.warn.customNameMatchesPredefinedMethod")).show();
                    model.getCustomFiatPaymentMethodName().set("");
                    return;
                }
                model.getAddedCustomFiatPaymentMethods().add(fiatPaymentMethod);
            }
            maybeAddFiatPaymentMethod(fiatPaymentMethod);
            model.getCustomFiatPaymentMethodName().set("");
        }
    }

    private boolean isPredefinedPaymentMethodsContainName(String name) {
        return new HashSet<>(PaymentMethodUtil.getPaymentMethodNames(model.getFiatPaymentMethods())).contains(name);
    }

    void onRemoveCustomMethod(FiatPaymentMethod fiatPaymentMethod) {
        model.getAddedCustomFiatPaymentMethods().remove(fiatPaymentMethod);
        model.getSelectedFiatPaymentMethods().remove(fiatPaymentMethod);
        model.getFiatPaymentMethods().remove(fiatPaymentMethod);
        setCookie();
    }

    private void setCookie() {
        settingsService.setCookie(CookieKey.CREATE_OFFER_METHODS, getCookieSubKey(),
                Joiner.on(",").join(PaymentMethodUtil.getPaymentMethodNames(model.getSelectedFiatPaymentMethods())));
    }

    private String getCookieSubKey() {
        return model.getMarket().get().getMarketCodes();
    }
}
