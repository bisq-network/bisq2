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

package bisq.desktop.main.content.user.accounts.create.account_data.method_forms;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public abstract class PaymentFormController implements Controller {
    protected final ServiceProvider serviceProvider;
    protected final Consumer<Map<String, Object>> dataChangeHandler;
    @Getter
    protected final PaymentFormView view;
    @Getter
    protected final Map<String, Object> formData = new HashMap<>();

    protected PaymentFormController(ServiceProvider serviceProvider,
                                    Consumer<Map<String, Object>> dataChangeHandler) {
        this.serviceProvider = serviceProvider;
        this.dataChangeHandler = dataChangeHandler;
        this.view = createView();
    }

    public boolean validate() {
        PaymentFormView formView = getView();
        try {
            Map<String, String> validationErrors = getValidationErrors();
            formView.showValidationErrors(validationErrors);
            return validationErrors.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public abstract Map<String, String> getValidationErrors();

    public void onFieldChanged(String fieldName, Object value) {
        formData.put(fieldName, value);
        dataChangeHandler.accept(formData);
    }

    public void restoreViewFromFormData() {
        updateViewFromFormData();
    }

    protected abstract void updateViewFromFormData();

    @Override
    public void onActivate() {
        updateViewFromFormData();
    }

    @Override
    public void onDeactivate() {
    }

    protected abstract PaymentFormView createView();
}