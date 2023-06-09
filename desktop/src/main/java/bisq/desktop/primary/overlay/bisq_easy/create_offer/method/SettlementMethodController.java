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
import javafx.collections.ObservableList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class SettlementMethodController implements Controller {
    private final SettlementMethodModel model;
    @Getter
    private final SettlementMethodView view;
    private Subscription customMethodPin;

    public SettlementMethodController(DefaultApplicationService applicationService) {
        model = new SettlementMethodModel();
        view = new SettlementMethodView(model, this);
    }

    /**
     * @return Enum names of FiatSettlement.Method or custom names
     */
    public ObservableList<String> getSettlementMethodNames() {
        return model.getSettlementMethodNames();
    }

    public void setMarket(Market market) {
        if (market == null) {
            return;
        }
        model.getSelectedMarket().set(market);
        model.getSettlementMethodNames().clear();
        List<FiatSettlement.Method> methods = FiatSettlement.getSettlementMethodsForCode(market.getQuoteCurrencyCode());
        model.getAllSettlementMethodNames().setAll(methods.stream().map(Enum::name).collect(Collectors.toList()));
        model.getAllSettlementMethodNames().addAll(model.getAddedCustomMethodNames());
        model.getIsSettlementMethodsEmpty().set(model.getAllSettlementMethodNames().isEmpty());
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

    void onToggleSettlementMethod(String methodName, boolean isSelected) {
        if (isSelected) {
            if (!model.getSettlementMethodNames().contains(methodName)) {
                model.getSettlementMethodNames().add(methodName);
            }
        } else {
            model.getSettlementMethodNames().remove(methodName);
        }
    }

    void onAddCustomMethod() {
        String customMethod = model.getCustomMethodName().get();
        boolean isEmpty = customMethod == null || customMethod.isEmpty();
        if (!isEmpty) {
            if (!model.getAddedCustomMethodNames().contains(customMethod)) {
                model.getAddedCustomMethodNames().add(customMethod);
            }
            if (!model.getSettlementMethodNames().contains(customMethod)) {
                model.getSettlementMethodNames().add(customMethod);
            }
            if (!model.getAllSettlementMethodNames().contains(customMethod)) {
                model.getAllSettlementMethodNames().add(customMethod);
            }

            model.getCustomMethodName().set("");
        }
    }

    void onRemoveCustomMethod(String customMethod) {
        model.getAddedCustomMethodNames().remove(customMethod);
        model.getSettlementMethodNames().remove(customMethod);
        model.getAllSettlementMethodNames().remove(customMethod);
    }
}
