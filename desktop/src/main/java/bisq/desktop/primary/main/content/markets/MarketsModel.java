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

package bisq.desktop.primary.main.content.markets;

import bisq.desktop.common.view.Model;
import bisq.oracle.marketprice.MarketPrice;
import bisq.presentation.formatters.QuoteFormatter;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.Map;

public class MarketsModel implements Model {
    final StringProperty formattedMarketPrice = new SimpleStringProperty("N/A");
    final StringProperty selectedCurrencyCode = new SimpleStringProperty("USD");

    public void setMarketPriceMap(Map<String, MarketPrice> marketPriceMap) {
        formattedMarketPrice.set(QuoteFormatter.format(marketPriceMap.get(selectedCurrencyCode.get()).quote()));
    }
}
