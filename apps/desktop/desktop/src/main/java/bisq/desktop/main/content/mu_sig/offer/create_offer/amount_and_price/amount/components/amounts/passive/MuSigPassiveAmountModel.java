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

package bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount.components.amounts.passive;

import bisq.common.market.Market;
import bisq.common.monetary.Monetary;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;

@Getter
public class MuSigPassiveAmountModel implements bisq.desktop.common.view.Model {
    private final boolean isLeftSideRangeAmount;
    private final ObjectProperty<Market> market = new SimpleObjectProperty<>();
    private final BooleanProperty isBaseCurrency = new SimpleBooleanProperty();
    private final ObjectProperty<Monetary> amount = new SimpleObjectProperty<>();
    private final StringProperty formattedAmount = new SimpleStringProperty();
    private final StringProperty code = new SimpleStringProperty();
    private final StringProperty tooltip = new SimpleStringProperty();
    private final BooleanProperty isBtc = new SimpleBooleanProperty();

    public MuSigPassiveAmountModel(boolean isLeftSideRangeAmount) {
        this.isLeftSideRangeAmount = isLeftSideRangeAmount;
    }

    void reset() {
        market.set(null);
        isBaseCurrency.set(false);
        amount.set(null);
        formattedAmount.set(null);
        code.set(null);
        tooltip.set(null);
        isBtc.set(false);
    }
}
