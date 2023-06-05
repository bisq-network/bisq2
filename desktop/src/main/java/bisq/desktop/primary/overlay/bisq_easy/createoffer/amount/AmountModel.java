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

package bisq.desktop.primary.overlay.bisq_easy.createoffer.amount;

import bisq.common.currency.Market;
import bisq.common.currency.MarketRepository;
import bisq.common.monetary.Coin;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.Quote;
import bisq.desktop.common.view.Model;
import bisq.offer.Direction;
import javafx.beans.property.*;
import lombok.Getter;
import lombok.Setter;

@Getter
public class AmountModel implements Model {
    private final Monetary minAmount = Coin.asBtc(10000);
    private final Monetary maxAmount = Coin.asBtc(1000000);
    private final double sliderMin = 0;
    private final double sliderMax = 1;

    private final ObjectProperty<Monetary> baseSideAmount = new SimpleObjectProperty<>();
    private final ObjectProperty<Monetary> quoteSideAmount = new SimpleObjectProperty<>();
    private final ObjectProperty<Quote> fixPrice = new SimpleObjectProperty<>();
    private final StringProperty spendOrReceiveString = new SimpleStringProperty();
    private final DoubleProperty sliderValue = new SimpleDoubleProperty();
    private final BooleanProperty sliderFocus = new SimpleBooleanProperty();
    @Setter
    private Market market = MarketRepository.getDefault();
    @Setter
    private Direction direction = Direction.BUY;

    AmountModel() {
    }

    void reset() {
        baseSideAmount.set(null);
        quoteSideAmount.set(null);
        fixPrice.set(null);
        spendOrReceiveString.set(null);
        sliderValue.set(0L);
        sliderFocus.set(false);
        market = MarketRepository.getDefault();
        direction = Direction.BUY;
    }
}