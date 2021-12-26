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

package network.misq.desktop.main.content.markets;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import network.misq.desktop.common.view.Model;
import network.misq.offer.MarketPrice;
import network.misq.presentation.formatters.QuoteFormatter;

import java.util.Map;

public class MarketsModel implements Model {
    final StringProperty formattedMarketPrice = new SimpleStringProperty("N/A");
    final StringProperty selectedCurrencyCode = new SimpleStringProperty("USD");

    public void setMarketPriceMap(Map<String, MarketPrice> marketPriceMap) {
        formattedMarketPrice.set(QuoteFormatter.format(marketPriceMap.get(selectedCurrencyCode.get()).quote()));
    }
}
