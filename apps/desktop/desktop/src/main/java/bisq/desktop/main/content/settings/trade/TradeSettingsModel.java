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

package bisq.desktop.main.content.settings.trade;

import bisq.desktop.common.converters.LongStringConverter;
import bisq.desktop.common.converters.PercentageStringConverter;
import bisq.desktop.common.view.Model;
import bisq.desktop.components.controls.validator.NumberValidator;
import bisq.desktop.components.controls.validator.PercentageValidator;
import bisq.desktop.components.controls.validator.ValidatorBase;
import bisq.i18n.Res;
import bisq.settings.SettingsService;
import javafx.beans.property.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Getter
public class TradeSettingsModel implements Model {
    private final BooleanProperty closeMyOfferWhenTaken = new SimpleBooleanProperty();
    private final DoubleProperty maxTradePriceDeviation = new SimpleDoubleProperty();
    private final IntegerProperty numDaysAfterRedactingTradeData = new SimpleIntegerProperty();

    private final PercentageStringConverter maxTradePriceDeviationConverter = new PercentageStringConverter(SettingsService.DEFAULT_MAX_TRADE_PRICE_DEVIATION);
    private final ValidatorBase maxTradePriceDeviationValidator =
            new PercentageValidator(Res.get("settings.trade.maxTradePriceDeviation.invalid",
                    SettingsService.MIN_TRADE_PRICE_DEVIATION, SettingsService.MAX_TRADE_PRICE_DEVIATION * 100),
                    SettingsService.MIN_TRADE_PRICE_DEVIATION, SettingsService.MAX_TRADE_PRICE_DEVIATION);

    private final LongStringConverter numDaysAfterRedactingTradeDataConverter = new LongStringConverter(SettingsService.DEFAULT_NUM_DAYS_AFTER_REDACTING_TRADE_DATA);
    private final ValidatorBase numDaysAfterRedactingTradeDataValidator =
            new NumberValidator(Res.get("settings.trade.numDaysAfterRedactingTradeData.invalid",
                    SettingsService.MIN_NUM_DAYS_AFTER_REDACTING_TRADE_DATA,
                    SettingsService.MAX_NUM_DAYS_AFTER_REDACTING_TRADE_DATA),
                    SettingsService.MIN_NUM_DAYS_AFTER_REDACTING_TRADE_DATA, SettingsService.MAX_NUM_DAYS_AFTER_REDACTING_TRADE_DATA);

    public TradeSettingsModel() {
    }
}
