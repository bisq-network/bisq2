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

package bisq.desktop.primary.overlay.createOffer.method;

import bisq.application.DefaultApplicationService;
import bisq.common.currency.Market;
import bisq.common.currency.PaymentMethodRepository;
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
    }

    public ObservableList<String> getPaymentMethods() {
        return model.getSelectedPaymentMethods();
    }

    public void setMarket(Market market) {
        if (market == null) {
            return;
        }
        model.getAllPaymentMethods().setAll(PaymentMethodRepository.getPaymentMethodsForMarket(market));
        model.getAllPaymentMethods().addAll(model.getAddedCustomMethods());
        model.getPaymentMethodsEmpty().set(model.getAllPaymentMethods().isEmpty());
    }

    @Override
    public void onActivate() {
        customMethodPin = EasyBind.subscribe(model.getCustomMethod(), customMethod -> model.getAddCustomMethodIconEnabled().set(customMethod != null && !customMethod.isEmpty()));
    }

    @Override
    public void onDeactivate() {
        customMethodPin.unsubscribe();
    }

    public void onTogglePaymentMethod(String paymentMethod, boolean isSelected) {
        if (isSelected) {
            if (!model.getSelectedPaymentMethods().contains(paymentMethod)) {
                model.getSelectedPaymentMethods().add(paymentMethod);
            }
        } else {
            model.getSelectedPaymentMethods().remove(paymentMethod);
        }
    }

    public void onAddCustomMethod() {
        String customMethod = model.getCustomMethod().get();
        boolean isEmpty = customMethod == null || customMethod.isEmpty();
        if (!isEmpty) {
            if (!model.getAddedCustomMethods().contains(customMethod)) {
                model.getAddedCustomMethods().add(customMethod);
            }
            if (!model.getSelectedPaymentMethods().contains(customMethod)) {
                model.getSelectedPaymentMethods().add(customMethod);
            }
            if (!model.getAllPaymentMethods().contains(customMethod)) {
                model.getAllPaymentMethods().add(customMethod);
            }

            model.getCustomMethod().set("");
        }
    }

    public void onRemoveCustomMethod(String customMethod) {
        model.getAddedCustomMethods().remove(customMethod);
        model.getSelectedPaymentMethods().remove(customMethod);
        model.getAllPaymentMethods().remove(customMethod);
    }
}
