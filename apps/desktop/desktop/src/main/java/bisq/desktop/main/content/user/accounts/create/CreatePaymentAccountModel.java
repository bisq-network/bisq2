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

package bisq.desktop.main.content.user.accounts.create;

import bisq.account.payment_method.PaymentMethod;
import bisq.desktop.common.view.NavigationModel;
import bisq.desktop.navigation.NavigationTarget;
import bisq.i18n.Res;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Getter
public class CreatePaymentAccountModel extends NavigationModel {
    private final IntegerProperty currentIndex = new SimpleIntegerProperty();
    private final StringProperty backButtonText = new SimpleStringProperty(Res.get("action.back"));
    private final BooleanProperty closeButtonVisible = new SimpleBooleanProperty();
    private final BooleanProperty nextButtonVisible = new SimpleBooleanProperty();
    private final BooleanProperty createAccountButtonVisible = new SimpleBooleanProperty();
    private final BooleanProperty backButtonVisible = new SimpleBooleanProperty();
    private final ObjectProperty<NavigationTarget> selectedChildTarget = new SimpleObjectProperty<>();
    private final List<NavigationTarget> childTargets = new ArrayList<>();

    @Setter
    private boolean optionsVisible;
    @Setter
    private boolean animateRightOut = true;
    @Setter
    private Optional<PaymentMethod<?>> paymentMethod = Optional.empty();
    @Setter
    private Optional<Map<String, Object>> accountData = Optional.empty();
    @Setter
    private Optional<Map<String, Object>> optionsData = Optional.empty();

    // Backward compatibility getters
    public PaymentMethod<?> getPaymentMethod() {
        return paymentMethod.orElse(null);
    }

    public Map<String, Object> getAccountData() {
        return accountData.orElse(null);
    }

    public Map<String, Object> getOptionsData() {
        return optionsData.orElse(null);
    }

    // Optional getters
    public Optional<PaymentMethod<?>> getPaymentMethodOpt() {
        return paymentMethod;
    }

    public Optional<Map<String, Object>> getAccountDataOpt() {
        return accountData;
    }

    public Optional<Map<String, Object>> getOptionsDataOpt() {
        return optionsData;
    }

    @Override
    public NavigationTarget getDefaultNavigationTarget() {
        return NavigationTarget.CREATE_PAYMENT_ACCOUNT_PAYMENT_METHOD;
    }

    public void reset() {
        optionsVisible = false;
        animateRightOut = true;
        paymentMethod = Optional.empty();
        accountData = Optional.empty();
        optionsData = Optional.empty();

        currentIndex.set(0);
        backButtonText.set(Res.get("action.back"));
        closeButtonVisible.set(false);
        nextButtonVisible.set(true);
        createAccountButtonVisible.set(false);
        backButtonVisible.set(false);
        selectedChildTarget.set(null);

        childTargets.clear();
    }
}