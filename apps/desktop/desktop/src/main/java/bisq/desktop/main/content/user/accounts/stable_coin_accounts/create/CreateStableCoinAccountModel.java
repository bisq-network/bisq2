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

package bisq.desktop.main.content.user.accounts.stable_coin_accounts.create;

import bisq.account.payment_method.stable_coin.StableCoinPaymentRail;
import bisq.desktop.common.view.NavigationModel;
import bisq.desktop.navigation.NavigationTarget;
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

@Getter
public class CreateStableCoinAccountModel extends NavigationModel {
    private final IntegerProperty currentIndex = new SimpleIntegerProperty();
    private final BooleanProperty createAccountButtonVisible = new SimpleBooleanProperty();
    private final BooleanProperty backButtonVisible = new SimpleBooleanProperty();
    private final BooleanProperty nextButtonDisabled = new SimpleBooleanProperty(true);
    private final BooleanProperty nextButtonVisible = new SimpleBooleanProperty();
    private final ObjectProperty<NavigationTarget> selectedChildTarget = new SimpleObjectProperty<>();
    private final List<NavigationTarget> childTargets = new ArrayList<>();
    @Setter
    private boolean animateRightOut = true;

    private final ObjectProperty<StableCoinPaymentRail> selectedRail = new SimpleObjectProperty<>();
    private final StringProperty address = new SimpleStringProperty("");
    private final StringProperty accountName = new SimpleStringProperty("");

    @Override
    public NavigationTarget getDefaultNavigationTarget() {
        return NavigationTarget.CREATE_STABLE_COIN_ACCOUNT_DATA;
    }
}
