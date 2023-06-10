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

package bisq.desktop.primary.overlay.bisq_easy.create_offer.price;

import bisq.common.currency.Market;
import bisq.desktop.common.view.Model;
import bisq.offer.price.MarketPriceSpec;
import bisq.offer.price.PriceSpec;
import javafx.beans.property.*;
import lombok.Getter;
import lombok.Setter;

@Getter
public class PriceModel implements Model {
    @Setter
    private Market market = null;
    private final DoubleProperty percentage = new SimpleDoubleProperty();
    private final StringProperty percentageAsString = new SimpleStringProperty();
    private final StringProperty priceAsString = new SimpleStringProperty();
    private final BooleanProperty useFixPrice = new SimpleBooleanProperty();
    private final ObjectProperty<PriceSpec> priceSpec = new SimpleObjectProperty<>(new MarketPriceSpec());

    public void reset() {
        market = null;
        percentageAsString.set(null);
        priceAsString.set(null);
        priceSpec.set(new MarketPriceSpec());
    }
}