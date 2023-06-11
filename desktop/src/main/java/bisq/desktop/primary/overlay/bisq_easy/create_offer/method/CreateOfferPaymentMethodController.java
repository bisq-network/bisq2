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

package bisq.desktop.primary.overlay.bisq_easy.create_offer.method;

import bisq.account.settlement.FiatSettlement;
import bisq.application.DefaultApplicationService;
import bisq.common.currency.Market;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;
import com.google.common.base.Joiner;
import javafx.collections.ObservableList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class CreateOfferPaymentMethodController implements Controller {
    private final CreateOfferPaymentMethodModel model;
    @Getter
    private final CreateOfferPaymentMethodView view;
    private final SettingsService settingsService;
    private Subscription customMethodPin;

    public CreateOfferPaymentMethodController(DefaultApplicationService applicationService) {
        settingsService = applicationService.getSettingsService();

        model = new CreateOfferPaymentMethodModel();
        view = new CreateOfferPaymentMethodView(model, this);
    }

    /**
     * @return Enum names of FiatSettlement.Method or custom names
     */
    public ObservableList<String> getSettlementMethodNames() {
        return model.getSelectedMethodNames();
    }

    public void setMarket(Market market) {
        if (market == null) {
            return;
        }

        model.getMarket().set(market);
        model.getSelectedMethodNames().clear();
        List<FiatSettlement.Method> methods = FiatSettlement.getSettlementMethodsForCode(market.getQuoteCurrencyCode());
        model.getAllMethodNames().setAll(methods.stream().map(Enum::name).collect(Collectors.toList()));
        model.getAllMethodNames().addAll(model.getAddedCustomMethodNames());
        model.getIsSettlementMethodsEmpty().set(model.getAllMethodNames().isEmpty());
    }

    public void reset() {
        model.reset();
    }

    @Override
    public void onActivate() {
        settingsService.getCookie().asString(CookieKey.CREATE_OFFER_METHODS, getCookieSubKey())
                .ifPresent(methodNames -> {
                    List.of(methodNames.split(",")).forEach(methodName -> {
                        if (model.getAllMethodNames().contains(methodName)) {
                            maybeAddMethodName(methodName);
                        } else {
                            maybeAddCustomMethod(methodName);
                        }
                    });
                });
        customMethodPin = EasyBind.subscribe(model.getCustomMethodName(),
                customMethod -> model.getIsAddCustomMethodIconEnabled().set(customMethod != null && !customMethod.isEmpty()));
    }

    @Override
    public void onDeactivate() {
        customMethodPin.unsubscribe();
    }

    void onToggleSettlementMethod(String methodName, boolean isSelected) {
        if (isSelected) {
            if (model.getSelectedMethodNames().size() >= 4) {
                new Popup().warning(Res.get("onboarding.method.warn.maxMethodsReached")).show();
                return;
            }
            maybeAddMethodName(methodName);
        } else {
            model.getSelectedMethodNames().remove(methodName);
            setCookie();
        }
    }


    void onAddCustomMethod() {
        if (model.getSelectedMethodNames().size() >= 4) {
            new Popup().warning(Res.get("onboarding.method.warn.maxMethodsReached")).show();
            return;
        }
        String customMethod = model.getCustomMethodName().get();
        maybeAddCustomMethod(customMethod);
    }

    private void maybeAddMethodName(String methodName) {
        if (!model.getSelectedMethodNames().contains(methodName)) {
            model.getSelectedMethodNames().add(methodName);
            setCookie();
        }
        if (!model.getAllMethodNames().contains(methodName)) {
            model.getAllMethodNames().add(methodName);
        }
    }

    private void maybeAddCustomMethod(String customMethod) {
        if (customMethod != null && !customMethod.isEmpty()) {
            if (!model.getAddedCustomMethodNames().contains(customMethod)) {
                model.getAddedCustomMethodNames().add(customMethod);
            }
            maybeAddMethodName(customMethod);

            model.getCustomMethodName().set("");
        }
    }

    void onRemoveCustomMethod(String customMethod) {
        model.getAddedCustomMethodNames().remove(customMethod);
        model.getSelectedMethodNames().remove(customMethod);
        model.getAllMethodNames().remove(customMethod);
        setCookie();
    }

    private void setCookie() {
        settingsService.setCookie(CookieKey.CREATE_OFFER_METHODS, getCookieSubKey(),
                Joiner.on(",").join(model.getSelectedMethodNames()));
    }

    private String getCookieSubKey() {
        return model.getMarket().get().getMarketCodes();
    }
}
