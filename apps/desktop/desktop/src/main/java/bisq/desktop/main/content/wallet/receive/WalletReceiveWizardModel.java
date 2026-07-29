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

package bisq.desktop.main.content.wallet.receive;

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
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Getter
public class WalletReceiveWizardModel extends NavigationModel {
    private final IntegerProperty currentIndex = new SimpleIntegerProperty();
    private final StringProperty nextButtonText = new SimpleStringProperty();
    private final BooleanProperty backButtonVisible = new SimpleBooleanProperty();
    private final ObjectProperty<NavigationTarget> selectedChildTarget = new SimpleObjectProperty<>();
    private final List<NavigationTarget> childTargets = List.of(
        NavigationTarget.WALLET_RECEIVE_ADDRESS,
        NavigationTarget.WALLET_ADDRESS_QR_CODE
    );
    @Setter
    private boolean animateRightOut = true;

    public WalletReceiveWizardModel() {
    }

    @Override
    public NavigationTarget getDefaultNavigationTarget() {
        return NavigationTarget.WALLET_RECEIVE_ADDRESS;
    }

    public void reset() {
        currentIndex.set(0);
        nextButtonText.set("");
        backButtonVisible.set(false);
        selectedChildTarget.set(null);
        animateRightOut = true;
    }
}
