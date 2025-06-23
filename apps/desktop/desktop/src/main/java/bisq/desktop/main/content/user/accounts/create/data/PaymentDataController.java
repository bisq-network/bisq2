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

package bisq.desktop.main.content.user.accounts.create.data;

import bisq.account.payment_method.FiatPaymentMethod;
import bisq.account.payment_method.FiatPaymentRail;
import bisq.account.payment_method.PaymentMethod;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.user.accounts.create.data.method_forms.F2FPaymentFormController;
import bisq.desktop.main.content.user.accounts.create.data.method_forms.PaymentFormController;
import bisq.desktop.main.content.user.accounts.create.data.method_forms.SepaPaymentFormController;
import javafx.beans.property.ReadOnlyObjectProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class PaymentDataController implements Controller {
    private final PaymentDataModel model;
    @Getter
    private final PaymentDataView view;
    private final ServiceProvider serviceProvider;

    private final Map<String, PaymentFormController> formControllerCache = new HashMap<>();
    private String currentControllerCacheKey;

    private Subscription paymentMethodPin;
    private PaymentFormController currentFormController;

    public PaymentDataController(ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
        model = new PaymentDataModel();
        view = new PaymentDataView(model, this);
    }

    @Override
    public void onActivate() {
        paymentMethodPin = EasyBind.subscribe(model.paymentMethodProperty(), this::onPaymentMethodChanged);
        Optional.ofNullable(model.getPaymentMethod().get())
                .ifPresent(this::createPaymentMethodForm);
    }

    @Override
    public void onDeactivate() {
        Optional.ofNullable(paymentMethodPin).ifPresent(pin -> {
            pin.unsubscribe();
            paymentMethodPin = null;
        });

        // Note: We intentionally do NOT deactivate the current form controller here
        // because we want to preserve its state when navigating away and back.
        // The form controllers will be properly cleaned up when the entire wizard is closed.
    }

    public void setPaymentMethod(PaymentMethod<?> paymentMethod) {
        model.setPaymentMethod(paymentMethod);
    }

    public ReadOnlyObjectProperty<Map<String, Object>> getAccountData() {
        return model.getAccountData();
    }

    public boolean validate() {
        return currentFormController != null && currentFormController.validate();
    }

    public void onFormDataChanged(Map<String, Object> data) {
        Map<String, Object> defensiveCopy = new HashMap<>(data);
        model.getAccountData().set(defensiveCopy);
    }

    public void reset() {
        Optional.ofNullable(currentFormController).ifPresent(controller -> {
            controller.onDeactivate();
            currentFormController = null;
            currentControllerCacheKey = null;
        });

        model.setPaymentMethod(null);
        model.getAccountData().set(new HashMap<>());
    }

    public void cleanup() {
        formControllerCache.values().forEach(PaymentFormController::onDeactivate);
        formControllerCache.clear();
        Optional.ofNullable(currentFormController).ifPresent(controller -> {
            controller.onDeactivate();
            currentFormController = null;
            currentControllerCacheKey = null;
        });
    }

    private void onPaymentMethodChanged(PaymentMethod<?> paymentMethod) {
        createPaymentMethodForm(paymentMethod);
    }

    private void createPaymentMethodForm(PaymentMethod<?> paymentMethod) {
        Optional.ofNullable(currentFormController).ifPresent(controller -> {
            String newKey = createCacheKey(paymentMethod);
            if (!newKey.equals(currentControllerCacheKey)) {
                log.debug("Deactivating previous form controller");
                controller.onDeactivate();
            }
        });

        currentFormController = createOrGetFormController(paymentMethod);

        Optional.ofNullable(currentFormController)
                .ifPresentOrElse(
                        controller -> {
                            currentControllerCacheKey = createCacheKey(paymentMethod);
                            view.setFormView(controller.getView());
                            controller.onActivate();

                            Map<String, Object> currentData = controller.getFormData();
                            onFormDataChanged(currentData);
                        },
                        () -> {
                            log.warn("Failed to create form controller for payment method: {}", paymentMethod);
                            currentControllerCacheKey = null;
                        }
                );
    }

    private PaymentFormController createOrGetFormController(PaymentMethod<?> paymentMethod) {
        String cacheKey = createCacheKey(paymentMethod);
        PaymentFormController cachedController = formControllerCache.get(cacheKey);
        if (cachedController != null) {
            return cachedController;
        }

        PaymentFormController newController = createNewFormController(paymentMethod);
        Optional.ofNullable(newController)
                .ifPresent(controller -> formControllerCache.put(cacheKey, controller));

        return newController;
    }

    private PaymentFormController createNewFormController(PaymentMethod<?> paymentMethod) {
        if (paymentMethod instanceof FiatPaymentMethod fiatMethod) {
            FiatPaymentRail rail = fiatMethod.getPaymentRail();

            return switch (rail) {
                case SEPA -> new SepaPaymentFormController(serviceProvider, this::onFormDataChanged);
                case F2F -> new F2FPaymentFormController(serviceProvider, this::onFormDataChanged);
                default -> {
                    log.warn("No specific form controller found for payment method: {}", rail);
                    yield null;
                }
            };
        }

        log.warn("Unsupported payment method type: {}", paymentMethod.getClass().getSimpleName());
        return null;
    }

    private String createCacheKey(PaymentMethod<?> paymentMethod) {
        return paymentMethod.getClass().getSimpleName() + "::" + paymentMethod.getName();
    }
}