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

package bisq.desktop.main.content.bisq_easy.trade_wizard.payment_methods;

import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.account.payment_method.FiatPaymentMethod;
import bisq.common.currency.Market;
import bisq.desktop.common.view.Model;
import bisq.offer.Direction;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import lombok.Getter;
import lombok.Setter;

@Getter
public class TradeWizardPaymentMethodsModel implements Model {
    @Setter
    private Direction direction;
    @Setter
    private String fiatSubtitleLabel;
    @Setter
    private String bitcoinSubtitleLabel;
    private final ObservableList<FiatPaymentMethod> fiatPaymentMethods = FXCollections.observableArrayList();
    private final SortedList<FiatPaymentMethod> sortedFiatPaymentMethods = new SortedList<>(fiatPaymentMethods);
    private final ObservableList<FiatPaymentMethod> selectedFiatPaymentMethods = FXCollections.observableArrayList();
    private final ObservableList<FiatPaymentMethod> addedCustomFiatPaymentMethods = FXCollections.observableArrayList();
    private final ObservableList<BitcoinPaymentMethod> bitcoinPaymentMethods = FXCollections.observableArrayList();
    private final SortedList<BitcoinPaymentMethod> sortedBitcoinPaymentMethods = new SortedList<>(bitcoinPaymentMethods);
    private final ObservableList<BitcoinPaymentMethod> selectedBitcoinPaymentMethods = FXCollections.observableArrayList();
    private final ObservableList<BitcoinPaymentMethod> addedCustomBitcoinPaymentMethods = FXCollections.observableArrayList();
    private final StringProperty customFiatPaymentMethodName = new SimpleStringProperty("");
    private final BooleanProperty isPaymentMethodsEmpty = new SimpleBooleanProperty();
    private final BooleanProperty canAddCustomFiatPaymentMethod = new SimpleBooleanProperty();
    private final ObjectProperty<Market> market = new SimpleObjectProperty<>();

    void reset() {
        direction = null;
        fiatSubtitleLabel = null;
        bitcoinSubtitleLabel = null;
        fiatPaymentMethods.clear();
        selectedFiatPaymentMethods.clear();
        addedCustomFiatPaymentMethods.clear();
        bitcoinPaymentMethods.clear();
        selectedBitcoinPaymentMethods.clear();
        customFiatPaymentMethodName.set("");
        isPaymentMethodsEmpty.set(false);
        canAddCustomFiatPaymentMethod.set(false);
        market.set(null);
    }
}
