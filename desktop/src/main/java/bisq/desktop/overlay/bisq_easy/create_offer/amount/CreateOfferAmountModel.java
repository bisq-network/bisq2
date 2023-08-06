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

package bisq.desktop.overlay.bisq_easy.create_offer.amount;

import bisq.common.currency.Market;
import bisq.common.currency.MarketRepository;
import bisq.desktop.common.view.Model;
import bisq.offer.amount.spec.AmountSpec;
import javafx.beans.property.*;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;

@Getter
public class CreateOfferAmountModel implements Model {
    private final BooleanProperty showRangeAmounts = new SimpleBooleanProperty();
    private final BooleanProperty isMinAmountEnabled = new SimpleBooleanProperty();
    private final StringProperty toggleButtonText = new SimpleStringProperty();
    private final ObjectProperty<AmountSpec> amountSpec = new SimpleObjectProperty<>();
    @Setter
    @Nullable
    private Market market = MarketRepository.getDefault();
    @Setter
    private String headline;

    public void reset() {
        isMinAmountEnabled.set(false);
        toggleButtonText.set(null);
    }
}