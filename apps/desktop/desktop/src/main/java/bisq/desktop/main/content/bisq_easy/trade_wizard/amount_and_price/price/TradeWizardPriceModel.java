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

package bisq.desktop.main.content.bisq_easy.trade_wizard.amount_and_price.price;

import bisq.common.currency.Market;
import bisq.common.monetary.PriceQuote;
import bisq.desktop.common.view.Model;
import bisq.offer.Direction;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.offer.price.spec.PriceSpec;
import javafx.beans.property.*;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;

@Getter
public class TradeWizardPriceModel implements Model {
    private final double maxPercentage = 0.5;
    private final double minPercentage = -0.1;
    private final double sliderMin = 0;
    private final double sliderMax = 1;
    @Setter
    private Market market = null;
    @Setter
    private Direction direction;
    private final DoubleProperty percentage = new SimpleDoubleProperty();
    private final StringProperty percentageInput = new SimpleStringProperty();
    private final StringProperty priceAsString = new SimpleStringProperty();
    private final BooleanProperty useFixPrice = new SimpleBooleanProperty();
    private final ObjectProperty<PriceSpec> priceSpec = new SimpleObjectProperty<>(new MarketPriceSpec());
    private final StringProperty errorMessage = new SimpleStringProperty();
    @Nullable
    @Setter
    private PriceQuote lastValidPriceQuote;
    private final StringProperty feedbackSentence = new SimpleStringProperty();
    private final BooleanProperty isOverlayVisible = new SimpleBooleanProperty();
    private final BooleanProperty shouldShowFeedback = new SimpleBooleanProperty();
    private final BooleanProperty shouldShowWarningIcon = new SimpleBooleanProperty();
    @Setter
    private boolean isFocused;
    private final DoubleProperty priceSliderValue = new SimpleDoubleProperty();
    private final BooleanProperty sliderFocus = new SimpleBooleanProperty();

    public void reset() {
        market = null;
        direction = null;
        percentage.set(0d);
        percentageInput.set(null);
        priceAsString.set(null);
        useFixPrice.set(false);
        priceSpec.set(null);
        errorMessage.set(null);
        lastValidPriceQuote = null;
        feedbackSentence.set(null);
        isOverlayVisible.set(false);
        shouldShowFeedback.set(false);
        isFocused = false;
        priceSliderValue.set(0d);
        sliderFocus.set(false);
    }
}
