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

package bisq.desktop.primary.overlay.bisq_easy.createoffer.method;

import bisq.account.settlement.FiatSettlement;
import bisq.application.DefaultApplicationService;
import bisq.common.currency.Market;
import bisq.desktop.common.view.Controller;
import javafx.collections.ObservableList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class PaymentMethodController implements Controller {
    private final PaymentMethodModel model;
    @Getter
    private final PaymentMethodView view;
    private Subscription customMethodPin;

    public PaymentMethodController(DefaultApplicationService applicationService) {
        model = new PaymentMethodModel();
        view = new PaymentMethodView(model, this);
        model.getSelectedMarket().addListener((observable, oldValue, newValue) -> model.getSelectedPaymentMethodNames().clear());
    }

    public ObservableList<String> getPaymentMethodNames() {
        return model.getSelectedPaymentMethodNames();
    }

    public void setMarket(Market market) {
        if (market == null) {
            return;
        }
        model.getSelectedMarket().set(market);
        model.getAllPaymentMethodNames().setAll(FiatSettlement.getPaymentMethodEnumNamesForCode(market.getQuoteCurrencyCode()));
        model.getAllPaymentMethodNames().addAll(model.getAddedCustomMethodNames());
        model.getIsPaymentMethodsEmpty().set(model.getAllPaymentMethodNames().isEmpty());
    }

    public void reset() {
        model.reset();
    }

    @Override
    public void onActivate() {
        customMethodPin = EasyBind.subscribe(model.getCustomMethodName(), customMethod -> model.getIsAddCustomMethodIconEnabled().set(customMethod != null && !customMethod.isEmpty()));
    }

    @Override
    public void onDeactivate() {
        customMethodPin.unsubscribe();
    }

    public void onTogglePaymentMethod(String paymentMethod, boolean isSelected) {
        if (isSelected) {
            if (!model.getSelectedPaymentMethodNames().contains(paymentMethod)) {
                model.getSelectedPaymentMethodNames().add(paymentMethod);
            }
        } else {
            model.getSelectedPaymentMethodNames().remove(paymentMethod);
        }
    }

    public void onAddCustomMethod() {
        String customMethod = model.getCustomMethodName().get();
        boolean isEmpty = customMethod == null || customMethod.isEmpty();
        if (!isEmpty) {
            if (!model.getAddedCustomMethodNames().contains(customMethod)) {
                model.getAddedCustomMethodNames().add(customMethod);
            }
            if (!model.getSelectedPaymentMethodNames().contains(customMethod)) {
                model.getSelectedPaymentMethodNames().add(customMethod);
            }
            if (!model.getAllPaymentMethodNames().contains(customMethod)) {
                model.getAllPaymentMethodNames().add(customMethod);
            }

            model.getCustomMethodName().set("");
        }
    }

    public void onRemoveCustomMethod(String customMethod) {
        model.getAddedCustomMethodNames().remove(customMethod);
        model.getSelectedPaymentMethodNames().remove(customMethod);
        model.getAllPaymentMethodNames().remove(customMethod);
    }
}
