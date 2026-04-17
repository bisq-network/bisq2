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

package bisq.desktop.main.content.mu_sig.offer.components.amount_selection;

import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.MonetaryRange;
import bisq.desktop.common.view.Model;
import bisq.offer.Direction;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
public class MuSigAmountSelectionModel implements Model {
    static final String SLIDER_TRACK_DEFAULT_COLOR = "-bisq-dark-grey-50";
    static final String SLIDER_TRACK_MARKER_COLOR = "-bisq2-green";
    static final int RANGE_INPUT_TEXT_MAX_LENGTH = 11;
    static final int FIXED_INPUT_TEXT_MAX_LENGTH = 18;

    private final Map<Integer, Integer> widthByNumCharsMap;
    @Setter
    private Market market = MarketRepository.getDefaultBtcFiatMarket();
    @Setter
    private Direction direction = Direction.BUY;

    // Range
    private final BooleanProperty useRangeAmount = new SimpleBooleanProperty();

    private final ObjectProperty<MonetaryRange> quoteSideTradeAmountLimits = new SimpleObjectProperty<>();
    private final ObjectProperty<MonetaryRange> tradeAmountLimitsInUsd = new SimpleObjectProperty<>();

    private final ObjectProperty<MonetaryRange> rangeBaseSideAmount = new SimpleObjectProperty<>();
    private final ObjectProperty<MonetaryRange> rangeQuoteSideAmount = new SimpleObjectProperty<>();

    private final ObjectProperty<Monetary> maxAllowedQuoteSideAmount = new SimpleObjectProperty<>();
    private final ObjectProperty<Monetary> maxAllowedBaseSideAmount = new SimpleObjectProperty<>();

    private final StringProperty formattedMinTradeAmountLimit = new SimpleStringProperty();
    private final StringProperty formattedMaxTradeAmountLimit = new SimpleStringProperty();
    private final StringProperty formattedMinTradeAmountLimitInUsd = new SimpleStringProperty();
    private final StringProperty formattedMaxTradeAmountLimitInUsd = new SimpleStringProperty();
    private final StringProperty tradeAmountLimitCode = new SimpleStringProperty();
    private final BooleanProperty showTradeAmountLimitInUsd = new SimpleBooleanProperty();


    // Amounts
    private final ObjectProperty<Monetary> minBaseSideAmount = new SimpleObjectProperty<>();
    private final ObjectProperty<Monetary> minQuoteSideAmount = new SimpleObjectProperty<>();
    private final ObjectProperty<Monetary> maxOrFixedBaseSideAmount = new SimpleObjectProperty<>();
    private final ObjectProperty<Monetary> maxOrFixedQuoteSideAmount = new SimpleObjectProperty<>();


    // Slider
    private final DoubleProperty maxOrFixedAmountSliderValue = new SimpleDoubleProperty();
    private final DoubleProperty minAmountSliderValue = new SimpleDoubleProperty();
    private final BooleanProperty maxOrFixedAmountSliderFocus = new SimpleBooleanProperty();
    private final BooleanProperty minAmountSliderFocus = new SimpleBooleanProperty();
    private final BooleanProperty rangeSliderLowThumbFocus = new SimpleBooleanProperty();
    private final BooleanProperty rangeSliderHighThumbFocus = new SimpleBooleanProperty();

    @Setter
    private Monetary leftMarkerQuoteSideValue;
    @Setter
    private Monetary rightMarkerQuoteSideValue;
    private final StringProperty sliderTrackStyle = new SimpleStringProperty();


    // Input type
    private final BooleanProperty isDefaultAmountInputBtc = new SimpleBooleanProperty(false);
    private final BooleanProperty shouldShowInvertedMinAmounts = new SimpleBooleanProperty(false);
    private final BooleanProperty shouldShowInvertedMaxOrFixedAmounts = new SimpleBooleanProperty(false);
    private final BooleanProperty shouldShowMaxOrFixedAmounts = new SimpleBooleanProperty(false);
    private final BooleanProperty shouldShowMinAmounts = new SimpleBooleanProperty(false);
    private final BooleanProperty showRangeAmountSelection = new SimpleBooleanProperty(false);

    // Strings
    private final StringProperty spendOrReceiveString = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();

    // Layout
    private final double sliderMin = 0;
    private final double sliderMax = 1;
    public final int amountBoxWidth = 300;
    public final int amountBoxHeight = 120;
    private final BooleanProperty shouldFocusInputTextField = new SimpleBooleanProperty(false);
    private final BooleanProperty shouldApplyNewInputTextFontStyle = new SimpleBooleanProperty(false);

    public MuSigAmountSelectionModel(Map<Integer, Integer> widthByNumCharsMap) {
        this.widthByNumCharsMap = widthByNumCharsMap;
    }

    void reset() {
        maxOrFixedBaseSideAmount.set(null);
        maxOrFixedQuoteSideAmount.set(null);
        minBaseSideAmount.set(null);
        minQuoteSideAmount.set(null);
        spendOrReceiveString.set(null);
        maxOrFixedAmountSliderValue.set(0L);
        minAmountSliderValue.set(0L);
        maxOrFixedAmountSliderFocus.set(false);
        minAmountSliderFocus.set(false);
        rangeSliderLowThumbFocus.set(false);
        rangeSliderHighThumbFocus.set(false);
        quoteSideTradeAmountLimits.set(null);
        tradeAmountLimitsInUsd.set(null);
        rangeBaseSideAmount.set(null);
        rangeQuoteSideAmount.set(null);
        maxAllowedQuoteSideAmount.set(null);
        maxAllowedBaseSideAmount.set(null);
        leftMarkerQuoteSideValue = null;
        rightMarkerQuoteSideValue = null;
        sliderTrackStyle.set(null);
        market = MarketRepository.getDefaultBtcFiatMarket();
        direction = Direction.BUY;
        description.set(null);
        formattedMinTradeAmountLimit.set(null);
        formattedMaxTradeAmountLimit.set(null);
        tradeAmountLimitCode.set(null);
        formattedMinTradeAmountLimitInUsd.set(null);
        formattedMaxTradeAmountLimitInUsd.set(null);
        showTradeAmountLimitInUsd.set(false);
        showRangeAmountSelection.set(false);
        isDefaultAmountInputBtc.set(false);
        shouldShowMinAmounts.set(false);
        shouldShowInvertedMinAmounts.set(false);
        shouldShowMaxOrFixedAmounts.set(false);
        shouldShowInvertedMaxOrFixedAmounts.set(false);
        shouldFocusInputTextField.set(false);
        shouldApplyNewInputTextFontStyle.set(false);
    }
}
