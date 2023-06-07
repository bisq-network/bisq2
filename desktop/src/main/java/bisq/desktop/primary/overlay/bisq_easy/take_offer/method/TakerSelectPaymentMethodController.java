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

package bisq.desktop.primary.overlay.bisq_easy.take_offer.method;

import bisq.account.settlement.FiatSettlement;
import bisq.application.DefaultApplicationService;
import bisq.common.currency.Market;
import bisq.desktop.common.view.Controller;
import bisq.offer.bisq_easy.BisqEasyOffer;
import javafx.collections.ObservableList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class TakerSelectPaymentMethodController implements Controller {
    private final TakerSelectPaymentMethodModel model;
    @Getter
    private final TakerSelectPaymentMethodView view;
    private Subscription customMethodPin;

    public TakerSelectPaymentMethodController(DefaultApplicationService applicationService) {
        model = new TakerSelectPaymentMethodModel();
        view = new TakerSelectPaymentMethodView(model, this);

        model.getSelectedMarket().addListener((observable, oldValue, newValue) -> model.getSelectedPaymentMethodNames().clear());
    }

    public void setBisqEasyOffer(BisqEasyOffer bisqEasyOffer) {

    }

    /**
     * @return Enum names of FiatSettlement.Method or custom names
     */
    public ObservableList<String> getPaymentMethodNames() {
        return model.getSelectedPaymentMethodNames();
    }

    public void setMarket(Market market) {
        if (market == null) {
            return;
        }
        model.getSelectedMarket().set(market);
        List<FiatSettlement.Method> methods = FiatSettlement.getSettlementMethodsForCode(market.getQuoteCurrencyCode());
        model.getAllPaymentMethodNames().setAll(methods.stream().map(Enum::name).collect(Collectors.toList()));
        model.getAllPaymentMethodNames().addAll(model.getAddedCustomMethodNames());
        model.getIsPaymentMethodsEmpty().set(model.getAllPaymentMethodNames().isEmpty());
    }

    public void reset() {
        model.reset();
    }

    @Override
    public void onActivate() {
        customMethodPin = EasyBind.subscribe(model.getCustomMethodName(),
                customMethod -> model.getIsAddCustomMethodIconEnabled().set(customMethod != null && !customMethod.isEmpty()));
    }

    @Override
    public void onDeactivate() {
        customMethodPin.unsubscribe();
    }

    public void onTogglePaymentMethod(String paymentMethodName, boolean isSelected) {
        if (isSelected) {
            if (!model.getSelectedPaymentMethodNames().contains(paymentMethodName)) {
                model.getSelectedPaymentMethodNames().add(paymentMethodName);
            }
        } else {
            model.getSelectedPaymentMethodNames().remove(paymentMethodName);
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
